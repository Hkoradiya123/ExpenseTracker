package com.example.expensetracker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPass;
    private Button btnLogin;
    private TextView mSignupHere, mForgotPassword;
    private ProgressDialog mDialog;

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private static final int REQ_ONE_TAP = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loginDetails();
        mAuth = FirebaseAuth.getInstance();
        mDialog = new ProgressDialog(this);

        // Initialize Google One Tap Sign-In
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_google_signin).setOnClickListener(v -> googleSignIn());
    }

    private void loginDetails() {
        mEmail  = findViewById(R.id.email_login);
        mPass = findViewById(R.id.password_login);
        btnLogin = findViewById(R.id.btn_login);
        mSignupHere = findViewById(R.id.signup_reg);
        mForgotPassword = findViewById(R.id.forgot_password);

        btnLogin.setOnClickListener(v -> {
            String email = mEmail.getText().toString().trim();
            String pass = mPass.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                mEmail.setError("Email required ...");
                Toast.makeText(getApplicationContext(), "Email required ...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(pass)) {
                mPass.setError("Password required ...");
                Toast.makeText(getApplicationContext(), "Password required ...", Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog.setMessage("Processing...");
            mDialog.show();

            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                mDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "LogIn Successful...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), HomeDetailsActivity.class));
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "LogIn Failed...", Toast.LENGTH_SHORT).show();
                }
            });
        });

        mSignupHere.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), RegistrationActivity.class)));
        mForgotPassword.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ResetActivity.class)));
    }

    private void googleSignIn() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(result.getPendingIntent().getIntentSender(), REQ_ONE_TAP, null, 0, 0, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(this, e -> Toast.makeText(getApplicationContext(), "Google Sign-In Failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONE_TAP) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken);
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getApplicationContext(), "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), HomeDetailsActivity.class));
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
