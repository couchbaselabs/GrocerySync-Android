package com.couchbase.grocerysync;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;


public class LoginActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private GoogleApiClient googleApiClient;

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
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton googleSignInButton = (SignInButton) findViewById(R.id.googleSignInButton);
        googleSignInButton.setSize(SignInButton.SIZE_STANDARD);
        googleSignInButton.setScopes(gso.getScopeArray());
        googleSignInButton.setOnClickListener(this);
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
        application.loginWithAuthCode(this);
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
                app.loginWithGoogleSignIn(this, idToken);
                success = true;
            } else
                errorMessage = "Google Sign-in failed : No ID Token returned";
        } else
            errorMessage = "Google Sign-in failed: (" +
                    result.getStatus().getStatusCode() + ") " +
                    result.getStatus().getStatusMessage();

        if (!success) {
            Application application = (Application) getApplication();
            application.showErrorMessage(errorMessage, null);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        String errorMessage = "Google Sign-in connection failed: (" +
                result.getErrorCode() + ") " +
                result.getErrorMessage();
        Application application = (Application) getApplication();
        application.showErrorMessage(errorMessage, null);
    }
}
