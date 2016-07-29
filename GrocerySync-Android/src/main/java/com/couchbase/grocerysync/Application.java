package com.couchbase.grocerysync;

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
    private static final String SERVER_DB_URL = "http://us-west.testfest.couchbasemobile.com:4984/grocery-sync/";

    interface ReplicationSetupCallback {
        void setup(Replication repl);
    }

    interface ReplicationChangeHandler {
        void change(Replication repl);
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

    private ReplicationChangeHandler changeHandler = null;

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

    private void stopReplication(boolean removeCredentials) {
        this.changeHandler = null;

        if (pull != null) {
            pull.stop();
            pull.removeChangeListener(this);
            if (removeCredentials)
                pull.clearAuthenticationStores();
            pull = null;
        }

        if (push != null) {
            push.stop();
            push.removeChangeListener(this);
            if (removeCredentials)
                push.clearAuthenticationStores();
            push = null;
        }
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        Replication repl = event.getSource();
        Log.d(TAG, "Replication Change Status: " + repl.getStatus() + " [ " + repl  + " ]");

        if (changeHandler != null)
            changeHandler.change(repl);

        Throwable error = null;
        if (pull != null)
            error = pull.getLastError();

        if (push != null) {
            if (error == null)
                error = push.getLastError();
        }

        if (error != syncError) {
            syncError = error;
            showMessage(syncError.getMessage(), null);
        }
    }

    public void loginWithAuthCode() {
        stopReplication(false);
        startPull(new ReplicationSetupCallback() {
            @Override
            public void setup(Replication repl) {
                OIDCLoginCallback callback =
                        OpenIDActivity.getOIDCLoginCallback(getApplicationContext());
                repl.setAuthenticator(
                        OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(
                                callback, context));

                // Setup a change handler to check if the pull replicator is done authenticating or not.
                // If done, start the push replicator
                changeHandler = new ReplicationChangeHandler() {
                    @Override
                    public void change(Replication repl) {
                        checkAuthCodeLogInComplete(repl);
                    }
                };
            }
        });
    }

    private void checkAuthCodeLogInComplete(Replication repl) {
        if (repl != pull)
            return;

        // Check the pull replicator is done authenticating or not.
        // If done, start the push replicator:
        if (username == null && repl.getUsername() != null && isReplicatorStarted(repl)) {
            if (login(repl.getUsername())) {
                if (pull != null) {
                    changeHandler = null;
                    startPush(new ReplicationSetupCallback() {
                        @Override
                        public void setup(Replication repl) {
                            OIDCLoginCallback callback =
                                    OpenIDActivity.getOIDCLoginCallback(getApplicationContext());
                            repl.setAuthenticator(
                                    OpenIDConnectAuthenticatorFactory.createOpenIDConnectAuthenticator(
                                            callback, context));
                        }
                    });
                    completeLogin();
                } else {
                    // Re authentication as database is deleted due to switching user:
                    loginWithAuthCode();
                }
            }
        }
    }

    private boolean isReplicatorStarted(Replication repl) {
        boolean isIdle = repl.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE;
        return isIdle || repl.getChangesCount() > 0;
    }

    public void loginWithGoogleSignIn(final String idToken) {
        // Send POST _session with the idToken to create a new SGW session:
        Request request = new Request.Builder()
                .url(getServerDbSessionUrl())
                .header("Authorization", "Bearer " + idToken)
                .post(new FormBody.Builder().build())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                showMessage("Failed to create a new SGW session with IDToken : " + idToken, e);
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
                        completeLogin();
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
                stopReplication(false);
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

    private void completeLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    public void logout() {
        stopReplication(true);

        this.username = null;

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LoginActivity.INTENT_ACTION_LOGOUT);
        startActivity(intent);
    }

    private void runOnUiThread(Runnable runnable) {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(runnable);
    }

    public void showMessage(final String message, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder(message);
                if (throwable != null) {
                    sb.append(": " + throwable);
                    Log.e(TAG, message, throwable);
                }
                Toast.makeText(getApplicationContext(), sb.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
