package com.couchbase.grocerysync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;


public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    public static final String INTENT_ACTION_LOGOUT = "logout";

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private GoogleApiClient googleApiClient;

    private static final String USE_GOOGLE_SIGN_IN_KEY = "UseGoogleSignIn";

    private boolean shouldContinueLogoutFromGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button authCodeSignInButton = (Button) findViewById(R.id.authCodeSignInButton);
        authCodeSignInButton.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton googleSignInButton = (SignInButton) findViewById(R.id.googleSignInButton);
        googleSignInButton.setSize(SignInButton.SIZE_STANDARD);
        googleSignInButton.setScopes(gso.getScopeArray());
        googleSignInButton.setOnClickListener(this);

        String action = getIntent().getAction();
        if (INTENT_ACTION_LOGOUT.equals(action)) {
            logout();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.authCodeSignInButton:
                authCodeSignIn();
                break;
            case R.id.googleSignInButton:
                googleSignIn();
                break;
        }
    }

    private void authCodeSignIn() {
        Application application = (Application)getApplication();
        application.loginWithAuthCode();
    }

    private void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleGoogleSignInResult(result);
        }
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {
        boolean success = false;
        String errorMessage = null;

        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            String idToken = acct.getIdToken();
            if (idToken != null) {
                Application app = (Application) getApplication();
                app.loginWithGoogleSignIn(idToken);
                success = true;
            } else
                errorMessage = "Google Sign-in failed : No ID Token returned";

            setLogInWithGoogleSignIn(true);
        } else
            errorMessage = "Google Sign-in failed: (" +
                    result.getStatus().getStatusCode() + ") " +
                    result.getStatus().getStatusMessage();

        if (!success) {
            Application application = (Application) getApplication();
            application.showMessage(errorMessage, null);
        }
    }

    private void setLogInWithGoogleSignIn(boolean used) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(USE_GOOGLE_SIGN_IN_KEY, used);
        editor.commit();
    }

    private boolean getLogInWithGoogleSignIn() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(USE_GOOGLE_SIGN_IN_KEY, false);
    }

    /** logout */
    private void logout() {
        if (getLogInWithGoogleSignIn())
            logoutFromGoogleSignIn();
        else {
            clearWebViewCookies();
            completeLogout();
        }
    }

    private void clearWebViewCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            cookieManager.removeAllCookie();
            CookieSyncManager.getInstance().sync();
        }
    }

    private void completeLogout() {
        Application application = (Application) getApplication();
        application.showMessage("Logout successfully", null);
    }

    private void logoutFromGoogleSignIn() {
        if (googleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
                Application application = (Application) getApplication();
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        clearWebViewCookies();
                        setLogInWithGoogleSignIn(false);
                        completeLogout();
                    } else
                        application.showMessage("Failed to sign out from Google Signin", null);
                }
            });
        } else {
            shouldContinueLogoutFromGoogleSignIn = true;
            if (!googleApiClient.isConnecting())
                googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (shouldContinueLogoutFromGoogleSignIn)
            logoutFromGoogleSignIn();
        shouldContinueLogoutFromGoogleSignIn = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        shouldContinueLogoutFromGoogleSignIn = false;

        String errorMessage = "Google Sign-in connection failed: (" +
                result.getErrorCode() + ") " +
                result.getErrorMessage();
        Application application = (Application) getApplication();
        application.showMessage(errorMessage, null);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }
}
