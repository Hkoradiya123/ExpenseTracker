package com.example.expensetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OneTapSignIn";

    private EditText mEmail, mPass;
    private Button btnLogin, btnGoogleLogin;
    private TextView mSignupHere, mForgotPassword;
    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        configureOneTapSignIn();
        setupUI();

        if (isUserLoggedIn()) {
            startActivity(new Intent(MainActivity.this, HomeDetailsActivity.class));
            finish();
        }
    }

    private void configureOneTapSignIn() {
        oneTapClient = Identity.getSignInClient(this);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id)) // Replace with your Web Client ID
                                .setFilterByAuthorizedAccounts(false) // Show all accounts
                                .build()
                )
                .setAutoSelectEnabled(true) // Automatically sign in if only one account is available
                .build();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void saveLoginStatus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    private void setupUI() {
        mEmail = findViewById(R.id.email_login);
        mPass = findViewById(R.id.password_login);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        mSignupHere = findViewById(R.id.signup_reg);
        mForgotPassword = findViewById(R.id.forgot_password);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> startOneTapSignIn());
        mSignupHere.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), RegistrationActivity.class)));
        mForgotPassword.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ResetActivity.class)));
    }

    private void loginWithEmail() {
        String email = mEmail.getText().toString().trim();
        String pass = mPass.getText().toString().trim();

        if (email.isEmpty()) {
            mEmail.setError(getString(R.string.email_required));
            return;
        }
        if (pass.isEmpty()) {
            mPass.setError(getString(R.string.password_required));
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveLoginStatus();
                startActivity(new Intent(getApplicationContext(), HomeDetailsActivity.class));
                finish();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOneTapSignIn() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> oneTapSignInLauncher.launch(
                        new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build()
                ))
                .addOnFailureListener(this, e -> Log.e(TAG, "One Tap Sign-In Failed: " + e.getLocalizedMessage()));
    }

    private final ActivityResultLauncher<IntentSenderRequest> oneTapSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();
                        firebaseAuthWithGoogle(idToken);
                    } catch (ApiException e) {
                        Log.e(TAG, "Error: " + e.getLocalizedMessage());
                    }
                }
            });

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String email = user.getEmail();
                    Toast.makeText(MainActivity.this, "Login Successful: " + email, Toast.LENGTH_LONG).show();
                    Log.d("AuthSuccess", "User signed in: " + email);

                    saveLoginStatus();
                    startActivity(new Intent(MainActivity.this, HomeDetailsActivity.class));
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Sign-in successful, but user is null!", Toast.LENGTH_LONG).show();
                    Log.e("AuthError", "FirebaseUser is null after sign-in");
                }
            } else {
                Exception e = task.getException();
                Toast.makeText(MainActivity.this, "Google Sign-In Failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                Log.e("AuthError", "Google Sign-In Failed", e);
            }
        });
    }


}
