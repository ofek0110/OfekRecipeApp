package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.example.ofek.utils.Validator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UserProfileActivity";
    private EditText etUserFirstName, etUserLastName, etUserEmail, etUserPhone, etUserPassword;
    private TextView tvUserDisplayName, tvUserDisplayEmail;
    private Button btnUpdateProfile, btnSignOut;
    private View adminBadge;
    private BottomNavigationView bottomNavigationView;

    String selectedUid;
    User selectedUser;
    boolean isCurrentUser = false;
    DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        // תיקון Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        User currentUser = SharedPreferencesUtil.getUser(this);

        if (currentUser == null) {
            finish();
            return;
        }

        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) {
            selectedUid = currentUser.getId();
        }
        isCurrentUser = selectedUid.equals(currentUser.getId());

        // בדיקת הרשאות
        if (!currentUser.isAdmin() && !isCurrentUser) {
            Toast.makeText(this, "You are not authorized to view this profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        showUserProfile();
    }

    private void initializeViews() {
        etUserFirstName = findViewById(R.id.et_user_first_name);
        etUserLastName = findViewById(R.id.et_user_last_name);
        etUserEmail = findViewById(R.id.et_user_email);
        etUserPhone = findViewById(R.id.et_user_phone);
        etUserPassword = findViewById(R.id.et_user_password);
        tvUserDisplayName = findViewById(R.id.tv_user_display_name);
        tvUserDisplayEmail = findViewById(R.id.tv_user_display_email);
        btnUpdateProfile = findViewById(R.id.btn_edit_profile);
        btnSignOut = findViewById(R.id.btn_sign_out);
        adminBadge = findViewById(R.id.admin_badge);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        btnUpdateProfile.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);

        if (!isCurrentUser) {
            btnSignOut.setVisibility(View.GONE);
        }

        // הגדרת ניווט תחתון
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(UserProfile.this, MainActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_profile) {
            updateUserProfile();
        } else if (v.getId() == R.id.btn_sign_out) {
            signOut();
        }
    }

    private void showUserProfile() {
        databaseService.getUser(selectedUid, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                selectedUser = user;
                etUserFirstName.setText(user.getFirstname());
                etUserLastName.setText(user.getLastname());
                etUserEmail.setText(user.getEmail());
                etUserPhone.setText(user.getPhone());
                etUserPassword.setText(user.getPassword());

                tvUserDisplayName.setText(user.getFirstname() + " " + user.getLastname());
                tvUserDisplayEmail.setText(user.getEmail());

                adminBadge.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error getting user profile", e);
            }
        });

        if (!isCurrentUser) {
            etUserEmail.setEnabled(false);
            etUserPassword.setEnabled(false);
        }
    }

    private void updateUserProfile() {
        if (selectedUser == null) return;

        String firstName = etUserFirstName.getText().toString();
        String lastName = etUserLastName.getText().toString();
        String phone = etUserPhone.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        if (!isValid(firstName, lastName, phone, email, password)) return;

        selectedUser.setFirstname(firstName);
        selectedUser.setLastname(lastName);
        selectedUser.setPhone(phone);
        selectedUser.setEmail(email);
        selectedUser.setPassword(password);

        databaseService.updateUser(selectedUser, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                Toast.makeText(UserProfile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                showUserProfile();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfile.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValid(String firstName, String lastName, String phone, String email, String password) {
        if (!Validator.isNameValid(firstName)) { etUserFirstName.setError("Required"); return false; }
        if (!Validator.isNameValid(lastName)) { etUserLastName.setError("Required"); return false; }
        if (!Validator.isPhoneValid(phone)) { etUserPhone.setError("Required"); return false; }
        if (!Validator.isEmailValid(email)) { etUserEmail.setError("Required"); return false; }
        if (!Validator.isPasswordValid(password)) { etUserPassword.setError("Required"); return false; }
        return true;
    }

    private void signOut() {
        SharedPreferencesUtil.signOutUser(this);
        Intent landingIntent = new Intent(this, LandingActivity.class);
        landingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(landingIntent);
    }
}