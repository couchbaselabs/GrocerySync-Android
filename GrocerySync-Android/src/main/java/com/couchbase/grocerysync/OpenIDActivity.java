package com.couchbase.grocerysync;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.couchbase.lite.auth.OIDCLoginCallback;
import com.couchbase.lite.auth.OIDCLoginContinuation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OpenIDActivity extends AppCompatActivity {
    private static final Map<String, OIDCLoginContinuation> continuationMap = new HashMap<>();

    public static final String INTENT_LOGIN_URL = "loginUrl";
    public static final String INTENT_REDIRECT_URL = "redirectUrl";
    public static final String INTENT_CONTINUATION_KEY = "continuationKey";

    private static final boolean MAP_LOCALHOST_TO_DB_SERVER_HOST = true;

    private String loginUrl;
    private String redirectUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_id);

        Intent intent = getIntent();
        loginUrl = intent.getStringExtra(INTENT_LOGIN_URL);
        redirectUrl = intent.getStringExtra(INTENT_REDIRECT_URL);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new OpenIdWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(loginUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_open_id, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_id_cancel:
                cancel();
                return true;
        }
        return false;
    }

    private void cancel() {
        Intent intent = getIntent();
        String key = intent.getStringExtra(INTENT_CONTINUATION_KEY);
        OpenIDActivity.deregisterLoginContinuation(key);
        finish();
    }

    private void didFinishAuthentication(String url, String error, String description) {
        Intent intent = getIntent();
        String key = intent.getStringExtra(INTENT_CONTINUATION_KEY);
        if (key != null) {
            OIDCLoginContinuation continuation =
                    OpenIDActivity.getLoginContinuation(key);

            URL authUrl = null;
            if (url != null) {
                try {
                    authUrl = new URL(url);

                    // Workaround for localhost development and test with Android emulators
                    // when the providers such as Google don't allow the callback host to be
                    // a non public domain (e.g. IP addresses):
                    if (authUrl.getHost().equals("localhost") && MAP_LOCALHOST_TO_DB_SERVER_HOST) {
                        Application application = (Application) getApplication();
                        String serverHost = application.getServerDbUrl().getHost();
                        authUrl = new URL(authUrl.toExternalForm().replace("localhost", serverHost));
                    }
                } catch (MalformedURLException e) { /* Shouldn't happen */ }
            }

            continuation.callback(authUrl, (error != null ? new Exception(error) : null));
        }
        OpenIDActivity.deregisterLoginContinuation(key);
    }

    private class OpenIdWebViewClient extends WebViewClient {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (shouldOverrideUrlLoading(request.getUrl()))
                return true;
            else
                return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlStr) {
            Uri url = Uri.parse(urlStr);
            if (shouldOverrideUrlLoading(url))
                return true;
            else
                return super.shouldOverrideUrlLoading(view, urlStr);
        }

        public boolean shouldOverrideUrlLoading(Uri url) {
            String urlStr = url.toString();
            if (urlStr.startsWith(redirectUrl)) {
                String error = null;
                String description = null;
                Set<String> queryNames = url.getQueryParameterNames();
                if (queryNames != null) {
                    for (String name : queryNames) {
                        if (name.equals("error"))
                            error = url.getQueryParameter(name);
                        else if (name.equals("error_description"))
                            description = url.getQueryParameter(name);
                    }
                }
                didFinishAuthentication(urlStr, error, description);
                return true;
            }
            return false;
        }
    }

    /** Manage OIDCLoginContinuation callback object */

    public static String registerLoginContinuation(OIDCLoginContinuation continuation) {
        String key = UUID.randomUUID().toString();
        continuationMap.put(key, continuation);
        return key;
    }

    public static OIDCLoginContinuation getLoginContinuation(String key) {
        return continuationMap.get(key);
    }

    public static void deregisterLoginContinuation(String key) {
        continuationMap.remove(key);
    }

    /** A factory to create OIDCLoginCallback callback */

    public static OIDCLoginCallback getOIDCLoginCallback(final Context context) {
        OIDCLoginCallback callback = new OIDCLoginCallback() {
            @Override
            public void callback(final URL loginURL,
                                 final URL redirectURL,
                                 final OIDCLoginContinuation cont) {
                // Ensure to run the code on the UI Thread:
                Handler handler = new Handler(context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String continuationKey = OpenIDActivity.registerLoginContinuation(cont);
                        Intent intent = new Intent(context, OpenIDActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(OpenIDActivity.INTENT_LOGIN_URL, loginURL.toExternalForm());
                        intent.putExtra(OpenIDActivity.INTENT_REDIRECT_URL, redirectURL.toExternalForm());
                        intent.putExtra(OpenIDActivity.INTENT_CONTINUATION_KEY, continuationKey);
                        context.startActivity(intent);
                    }
                });
            }
        };
        return callback;
    }
}
