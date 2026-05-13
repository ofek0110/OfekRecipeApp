package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.example.ofek.utils.Validator;

public class LogIn extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    TextView Btn_landing_go_to_create;
    EditText etEmail, etPassword;
    Button btnSubmit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvRegisterLogin = findViewById(R.id.Btn_landing_go_to_create);

        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnSubmit = findViewById(R.id.btn_login_login);

        tvRegisterLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogIn.this, Register.class);
                LogIn.this.startActivity(intent);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Login button clicked");

                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();

                if (!checkInput(email, password)) {
                    return;
                }

                loginUser(email, password);
            }
        });
    }

    private boolean checkInput(String email, String password) {
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("Password must be at least 6 characters long");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        DatabaseService databaseService = DatabaseService.getInstance();
        databaseService.getUserByEmailAndPassword(email, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user == null) {
                    etPassword.setError("Invalid email or password");
                    etPassword.requestFocus();
                    return;
                }
                SharedPreferencesUtil.saveUser(LogIn.this, user);
                // Redirect to MainContainerActivity instead of MainActivity
                Intent mainIntent = new Intent(LogIn.this, MainContainerActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                etPassword.setError("Invalid email or password");
                etPassword.requestFocus();
                SharedPreferencesUtil.signOutUser(LogIn.this);
            }
        });
    }
}