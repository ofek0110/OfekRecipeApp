package com.example.ofek.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.example.ofek.utils.Validator;

import java.util.function.UnaryOperator;

public class UserProfile extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UserProfileActivity";
    private EditText etUserFirstName, etUserLastName, etUserEmail, etUserPhone, etUserPassword;
    private TextView tvUserDisplayName, tvUserDisplayEmail;
    private Button btnUpdateProfile, btnSignOut, btnDeleteUser;
    private View adminBadge;

    String selectedUid;
    User selectedUser;
    boolean isCurrentUser = false;
    DatabaseService databaseService;
    User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        currentUser = SharedPreferencesUtil.getUser(this);

        if (currentUser == null) {
            finish();
            return;
        }

        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) {
            selectedUid = currentUser.getId();
        }
        isCurrentUser = selectedUid.equals(currentUser.getId());

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
        btnDeleteUser = findViewById(R.id.btn_delete_user);
        adminBadge = findViewById(R.id.admin_badge);

        btnUpdateProfile.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);
        btnDeleteUser.setOnClickListener(this);

        // Hide sign out if viewing another user's profile
        if (!isCurrentUser) {
            btnSignOut.setVisibility(View.GONE);
        }

        // Hide bottom navigation if it exists in the layout
        View nav = findViewById(R.id.bottomNavigation);
        if (nav != null) nav.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_profile) {
            updateUserProfile();
        } else if (v.getId() == R.id.btn_sign_out) {
            signOut();
        } else if (v.getId() == R.id.btn_delete_user) {
            confirmAndDeleteUser();
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

        // ניהול הרשאות עריכה ומחיקה
        if (!isCurrentUser) {
            if (currentUser.isAdmin()) {
                // המנהל מקבל גישה חופשית לעריכת כל השדות של המשתמש האחר
                etUserFirstName.setEnabled(true);
                etUserLastName.setEnabled(true);
                etUserPhone.setEnabled(true);
                etUserEmail.setEnabled(true);
                etUserPassword.setEnabled(true);
                btnUpdateProfile.setVisibility(View.VISIBLE);
                btnDeleteUser.setVisibility(View.VISIBLE); // הצגת כפתור המחיקה למנהל בלבד
            } else {
                // משתמש רגיל לא יכול לערוך שום שדה של משתמש אחר
                etUserFirstName.setEnabled(false);
                etUserLastName.setEnabled(false);
                etUserPhone.setEnabled(false);
                etUserEmail.setEnabled(false);
                etUserPassword.setEnabled(false);
                btnUpdateProfile.setVisibility(View.GONE);
                btnDeleteUser.setVisibility(View.GONE);
            }
        } else {
            // המשתמש הנוכחי עורך את עצמו
            etUserFirstName.setEnabled(true);
            etUserLastName.setEnabled(true);
            etUserPhone.setEnabled(true);
            etUserEmail.setEnabled(true);
            etUserPassword.setEnabled(true);
            btnUpdateProfile.setVisibility(View.VISIBLE);
            btnDeleteUser.setVisibility(View.GONE); // מניעת מחיקה עצמית מכאן
        }
    }

    private void confirmAndDeleteUser() {
        if (selectedUid == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to completely delete this user from the database?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseService.deleteUser(selectedUid, new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void object) {
                                Toast.makeText(UserProfile.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                                finish(); // סגירת המסך וחזרה לרשימה לאחר מחיקה
                            }

                            @Override
                            public void onFailed(Exception e) {
                                Toast.makeText(UserProfile.this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        databaseService.updateUser(selectedUid, new UnaryOperator<User>() {
            @Override
            public User apply(User user) {
                if (user != null) {
                    user.setFirstname(selectedUser.getFirstname());
                    user.setLastname(selectedUser.getLastname());
                    user.setPhone(selectedUser.getPhone());
                    user.setEmail(selectedUser.getEmail());
                    user.setPassword(selectedUser.getPassword());
                }
                return user;
            }
        }, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
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
        finish();
    }
}