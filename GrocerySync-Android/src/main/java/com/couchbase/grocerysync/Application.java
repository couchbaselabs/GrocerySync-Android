package com.couchbase.grocerysync;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.OIDCLoginCallback;
import com.couchbase.lite.auth.OpenIDConnectAuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Application extends android.app.Application implements Replication.ChangeListener {
    public static final String TAG = "GrocerySync";

    private static final String DATABASE_NAME = "grocery-sync";
    private static final String USER_LOCAL_DOC_ID = "user";
    private static final String SERVER_DB_URL = "http://<HOST>:<PORT>/grocery-sync";

    interface ReplicationSetupCallback {
        void setup(Replication repl);
    }

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    private AndroidContext context;
    private Manager manager;
    private Database database;
    private Replication pull;
    private Replication push;
    private Throwable syncError;

    private String username;

    private boolean shouldStartPushAfterPullStart = false;
    private int pullIdleCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeDatabase();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public Database getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    private boolean initializeDatabase() {
        Manager.enableLogging(TAG, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC_ASYNC_TASK, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_QUERY, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_VIEW, com.couchbase.lite.util.Log.VERBOSE);
        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_DATABASE, com.couchbase.lite.util.Log.VERBOSE);

        if (manager == null) {
            try {
                context = new AndroidContext(getApplicationContext());
                manager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't create manager object", e);
                return false;
            }
        }

        if (database == null) {
            DatabaseOptions options = new DatabaseOptions();
            options.setStorageType(Manager.SQLITE_STORAGE);
            options.setCreate(true);
            try {
                database = manager.openDatabase(DATABASE_NAME, options);
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Couldn't open database", e);
                return false;
            }
        }
        return true;
    }

    public URL getServerDbUrl() {
        try {
            return new URL(SERVER_DB_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getServerDbSessionUrl() {
        String serverUrl = SERVER_DB_URL;
        if (!serverUrl.endsWith("/"))
            serverUrl = serverUrl + "/";
        try {
            return new URL(serverUrl + "_session");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void startPull(ReplicationSetupCallback callback) {
        pullIdleCount = 0;
        pull = database.createPullReplication(getServerDbUrl());
        pull.setContinuous(true);
        if (callback != null) callback.setup(pull);
        pull.addChangeListener(this);
        pull.start();
    }

    private void startPush(ReplicationSetupCallback callback) {
        push = database.createPushReplication(getServerDbUrl());
        push.setContinuous(true);
        if (callback != null) callback.setup(push);
        push.addChangeListener(this);
        push.start();
    }

    private void stopReplication() {
        if (pull != null) {
            pull.stop();
            pull.removeChangeListener(this);
            pull = null;
        }

        if (push != null) {
            push.stop();
            push.removeChangeListener(this);
            push = null;
        }
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        Replication repl = event.getSource();
        Log.v(TAG, "Replication Change Status: " + repl.getStatus() + " [ " + repl  + " ]");
        Throwable error = null;
        if (pull != null) {
            error = pull.getLastError();
            if (shouldStartPushAfterPullStart && isReplicatorStartedOrError(pull)) {
                boolean needRelogin = false;
                if (error == null) {
                    String username = pull.getAuthenticator().getUsername();
                    if (login(username)) {
                        if (pull == repl) {
                            startPush(new ReplicationSetupCallback() {
                                @Override
                                public void setup(Replication repl) {
                                    OIDCLoginCallback callback =
                                            OpenIDAuthenticator.getOIDCLoginCallback(getApplicationContext());
                                    repl.setAuthenticator(
                                            OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(
                                                    callback, context));
                                }
                            });
                            startApplication();
                        } else
                            needRelogin = true;
                    }
                }
                shouldStartPushAfterPullStart = false;

                if (needRelogin) {
                    loginWithAuthCode();
                    return;
                }
            }
        }

        if (push != null) {
            if (error == null)
                error = push.getLastError();
        }

        if (error != syncError) {
            syncError = error;
            showErrorMessage(syncError.getMessage(), null);
        }
    }

    private boolean isReplicatorStartedOrError(Replication repl) {
        boolean isIdle;
        if (repl == pull) {
            isIdle = repl.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE;
            isIdle = isIdle && (++pullIdleCount > 1);
        } else {
            isIdle = repl.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE;
        }
        return isIdle || repl.getChangesCount() > 0 || repl.getLastError() != null;
    }

    public void loginWithAuthCode() {
        stopReplication();
        startPull(new ReplicationSetupCallback() {
            @Override
            public void setup(Replication repl) {
                shouldStartPushAfterPullStart = true;
                OIDCLoginCallback callback =
                        OpenIDAuthenticator.getOIDCLoginCallback(getApplicationContext());
                repl.setAuthenticator(
                        OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(
                                callback, context));
            }
        });
    }

    public void loginWithGoogleSignIn(final Activity activity, final String idToken) {
        Request request = new Request.Builder()
                .url(getServerDbSessionUrl())
                .header("Authorization", "Bearer " + idToken)
                .post(new FormBody.Builder().build())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> session = gson.fromJson(response.body().charStream(), type);
                    Map<String, Object> userInfo = (Map<String, Object>) session.get("userCtx");
                    final String username = (userInfo != null ? (String) userInfo.get("name") : null);
                    final List<Cookie> cookies =
                            Cookie.parseAll(HttpUrl.get(getServerDbUrl()), response.headers());
                    if (login(username, cookies)) {
                        startApplication();
                    }
                }
            }
        });
    }

    private boolean login(String username, final List<Cookie>sessionCookies) {
        if (login(username)) {
            startPull(new ReplicationSetupCallback() {
                @Override
                public void setup(Replication repl) {
                    for (Cookie cookie : sessionCookies) {
                        repl.setCookie(cookie.name(), cookie.value(), cookie.path(),
                                new Date(cookie.expiresAt()), cookie.secure(), cookie.httpOnly());
                    }
                }
            });
            startPush(new ReplicationSetupCallback() {
                @Override
                public void setup(Replication repl) {
                    for (Cookie cookie : sessionCookies) {
                        repl.setCookie(cookie.name(), cookie.value(), cookie.path(),
                                new Date(cookie.expiresAt()), cookie.secure(), cookie.httpOnly());
                    }
                }
            });
            return true;
        }
        return false;
    }

    private boolean login(String username) {
        if (username == null)
            return false;

        if (database != null) {
            Map<String, Object> user = database.getExistingLocalDocument(USER_LOCAL_DOC_ID);
            if (user != null && !username.equals(user.get("username"))) {
                stopReplication();
                try {
                    database.delete();
                } catch (CouchbaseLiteException e) {
                    return false;
                }
                database = null;
            }
        }

        if (database == null) {
            if (!initializeDatabase())
                return false;
        }

        Map<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.put("username", username);
        try {
            database.putLocalDocument(USER_LOCAL_DOC_ID, userInfo);
        } catch (CouchbaseLiteException e) {
            return false;
        }

        this.username = username;

        return true;
    }

    private void startApplication() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(runnable);
    }

    public void showErrorMessage(final String errorMessage, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, errorMessage, throwable);
                String msg = String.format("%s: %s",
                        errorMessage, throwable != null ? throwable : "");
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
