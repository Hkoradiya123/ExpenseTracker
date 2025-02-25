package com.example.expensetracker;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPass;
    private Button btnLogin;
    private TextView mSignupHere, mForgotPassword;
    private ProgressDialog mDialog;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication FIRST
        loginDetails(); // Now call loginDetails()

        if (isUserLoggedIn()) {
            startActivity(new Intent(MainActivity.this, HomeDetailsActivity.class));
            finish();
        }
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

    private void loginDetails() {
        mEmail = findViewById(R.id.email_login);
        mPass = findViewById(R.id.password_login);
        btnLogin = findViewById(R.id.btn_login);
        mSignupHere = findViewById(R.id.signup_reg);
        mForgotPassword = findViewById(R.id.forgot_password);

        mDialog = new ProgressDialog(this);

        btnLogin.setOnClickListener(v -> {
            String email = mEmail.getText().toString().trim();
            String pass = mPass.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                mEmail.setError(getString(R.string.email_required));
                Toast.makeText(getApplicationContext(), getString(R.string.email_required), Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(pass)) {
                mPass.setError(getString(R.string.password_required));
                Toast.makeText(getApplicationContext(), getString(R.string.password_required), Toast.LENGTH_SHORT).show();
                return;
            }

            mDialog.setMessage(getString(R.string.processing));
            mDialog.show();

            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                mDialog.dismiss();
                if (task.isSuccessful()) {
                    saveLoginStatus(); // Save only on successful login
                    Toast.makeText(getApplicationContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), HomeDetailsActivity.class));
                    finish(); // Close MainActivity after login
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Registration Activity
        mSignupHere.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), RegistrationActivity.class)));

        // Reset Password Activity
        mForgotPassword.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ResetActivity.class)));
    }
}
