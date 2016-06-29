package com.couchbase.grocerysync;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.couchbase.lite.auth.OpenIDConnectAuthorizer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OpenIDAuthenticator {
    private static final Map<String, OpenIDConnectAuthorizer.OIDCLoginContinuation> continuationMap
            = new HashMap<>();

    public static String registerLoginContinuation(OpenIDConnectAuthorizer.OIDCLoginContinuation continuation) {
        String key = UUID.randomUUID().toString();
        continuationMap.put(key, continuation);
        return key;
    }

    public static OpenIDConnectAuthorizer.OIDCLoginContinuation getLoginContinuation(String key) {
        return continuationMap.get(key);
    }

    public static void deregisterLoginContinuation(String key) {
        continuationMap.remove(key);
    }

    public static OpenIDConnectAuthorizer.OIDCLoginCallback getOIDCLoginCallback(final Context context) {
        OpenIDConnectAuthorizer.OIDCLoginCallback callback =
            new OpenIDConnectAuthorizer.OIDCLoginCallback() {
                @Override
                public void callback(final URL loginURL,
                                     final URL redirectURL,
                                     final OpenIDConnectAuthorizer.OIDCLoginContinuation continuation) {
                    runOnUiThread(context, new Runnable() {
                        @Override
                        public void run() {
                            String continuationKey = registerLoginContinuation(continuation);
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

    private static void runOnUiThread(Context context, Runnable runnable) {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(runnable);
    }
}
