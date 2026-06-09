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

// אקטיביטי פרופיל משתמש דינמי המיועד לשני שימושים: הצגת/עדכון הפרופיל האישי של המשתמש הנוכחי,
// או פתיחת פרופיל של משתמש אחר עבור האדמין (כולל אפשרות למחיקת המשתמש).
public class UserProfile extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "UserProfileActivity";
    private EditText etUserFirstName, etUserLastName, etUserEmail, etUserPhone, etUserPassword;
    private TextView tvUserDisplayName, tvUserDisplayEmail;
    private Button btnUpdateProfile, btnSignOut, btnDeleteUser;
    private View adminBadge;

    String selectedUid;
    User selectedUser;
    boolean isCurrentUser = false; // דגל המציין האם המשתמש צופה בפרופיל של עצמו
    DatabaseService databaseService;
    User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_profile);

        // סידור שולי התצוגה (Insets) למניעת חפיפה עם סרגלי המערכת
        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        databaseService = DatabaseService.getInstance();
        currentUser = SharedPreferencesUtil.getUser(this);

        if (currentUser == null) {
            finish();
            return;
        }

        // ניתוח ה-Intent: בדיקה האם הגענו לצפות במשתמש ספציפי (דרך ניהול אדמין) או בפרופיל של עצמנו
        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) {
            selectedUid = currentUser.getId();
        }
        isCurrentUser = selectedUid.equals(currentUser.getId());

        initializeViews();
        showUserProfile();
    }

    // אתחול רכיבי ה-UI, הגדרת מאזיני לחיצה, וביצוע התאמות ויזואליות ראשוניות לפי סוג הפרופיל
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

        if (btnUpdateProfile != null) btnUpdateProfile.setOnClickListener(this);
        if (btnSignOut != null) btnSignOut.setOnClickListener(this);
        if (btnDeleteUser != null) btnDeleteUser.setOnClickListener(this);

        if (!isCurrentUser && btnSignOut != null) {
            btnSignOut.setVisibility(View.GONE);
        }

        View nav = findViewById(R.id.bottomNavigation);
        if (nav != null) nav.setVisibility(View.GONE);
    }

    // ניהול מרכזי של אירועי הלחיצה במסך (עדכון, התנתקות, או מחיקה)
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_edit_profile) {
            updateUserProfile();
        } else if (id == R.id.btn_sign_out) {
            signOut();
        } else if (id == R.id.btn_delete_user) {
            confirmAndDeleteUser();
        }
    }

    // פנייה ל-Firebase לשליפת נתוני הפרופיל שנבחר, אכלוס השדות, והפעלת חוקי הרשאות עריכה/מחיקה (למשל חסימת עריכה לאדמין על פרופיל זר)
    private void showUserProfile() {
        if (selectedUid == null) return;

        databaseService.getUser(selectedUid, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user == null) return;
                selectedUser = user;
                etUserFirstName.setText(user.getFirstname());
                etUserLastName.setText(user.getLastname());
                etUserEmail.setText(user.getEmail());
                etUserPhone.setText(user.getPhone());
                etUserPassword.setText(user.getPassword());

                tvUserDisplayName.setText(user.getFirstname() + " " + user.getLastname());
                tvUserDisplayEmail.setText(user.getEmail());

                if (adminBadge != null) {
                    adminBadge.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error getting user profile", e);
            }
        });

        // לוגיקת נראות כפתור מחיקה: יוצג רק אם המשתמש המחובר הוא אדמין והוא צופה בפרופיל של מישהו אחר
        if (btnDeleteUser != null) {
            if (currentUser.isAdmin() && !isCurrentUser) {
                btnDeleteUser.setVisibility(View.VISIBLE);
            } else {
                btnDeleteUser.setVisibility(View.GONE);
            }
        }

        // הגבלת עריכה: אם זה לא המשתמש עצמו, ננעל את השדות לחלוטין (אדמין יכול למחוק משתמש אחר, אך לא לערוך את פרטיו)
        if (!isCurrentUser) {
            etUserFirstName.setEnabled(false);
            etUserLastName.setEnabled(false);
            etUserPhone.setEnabled(false);
            etUserEmail.setEnabled(false);
            etUserPassword.setEnabled(false);
            if (btnUpdateProfile != null) btnUpdateProfile.setVisibility(View.GONE);
        }
    }

    // מנגנון מחיקה (עבור אדמין): הצגת דיאלוג אזהרה ומחיקה מוחלטת של המשתמש הנוכחי מתיקיית ה-users ב-Firebase
    private void confirmAndDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to completely delete this user? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseService.deleteUser(selectedUid, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(UserProfile.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(UserProfile.this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // עדכון פרופיל עצמי: איסוף הקלטים, הרצת וולידציה ושליחת השינויים לשרת באמצעות טרנזקציה ב-DatabaseService
    private void updateUserProfile() {
        if (selectedUser == null) return;

        String firstName = etUserFirstName.getText().toString();
        String lastName = etUserLastName.getText().toString();
        String phone = etUserPhone.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        if (!isValid(firstName, lastName, phone, email, password)) return;

        databaseService.updateUser(selectedUid, user -> {
            if (user != null) {
                user.setFirstname(firstName);
                user.setLastname(lastName);
                user.setPhone(phone);
                user.setEmail(email);
                user.setPassword(password);
            }
            return user;
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

    // וולידציה: שימוש במחלקת ה-Validator כדי לוודא ששדות השם, הטלפון, האימייל והסיסמה תקינים
    private boolean isValid(String firstName, String lastName, String phone, String email, String password) {
        if (!Validator.isNameValid(firstName)) { etUserFirstName.setError("Required"); return false; }
        if (!Validator.isNameValid(lastName)) { etUserLastName.setError("Required"); return false; }
        if (!Validator.isPhoneValid(phone)) { etUserPhone.setError("Required"); return false; }
        if (!Validator.isEmailValid(email)) { etUserEmail.setError("Required"); return false; }
        if (!Validator.isPasswordValid(password)) { etUserPassword.setError("Required"); return false; }
        return true;
    }

    // התנתקות: ניקוי הזיכרון המקומי (SharedPreferences) והעברת המשתמש למסך הפתיחה תוך חסימת אפשרות חזרה
    private void signOut() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}