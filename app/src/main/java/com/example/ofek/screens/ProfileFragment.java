package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.example.ofek.utils.Validator;

import java.util.function.UnaryOperator;

// פרגמנט פרופיל המשתמש (חלק משלושת מסכי הליבה בקונטיינר הראשי).
// מאפשר למשתמש לצפות בפרטים האישיים שלו, לעדכן אותם מול השרת או להתנתק מהחשבון.
public class ProfileFragment extends Fragment implements View.OnClickListener {

    private EditText etUserFirstName, etUserLastName, etUserEmail, etUserPhone, etUserPassword;
    private TextView tvUserDisplayName, tvUserDisplayEmail;
    private Button btnUpdateProfile, btnSignOut;
    private View adminBadge;

    private User currentUser;
    private DatabaseService databaseService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        databaseService = DatabaseService.getInstance();

        currentUser = SharedPreferencesUtil.getUser(requireContext());
        if (currentUser == null) {
            Intent intent = new Intent(requireActivity(), LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        initializeViews(view);
        showUserProfile();
    }

    private void initializeViews(View view) {
        etUserFirstName = view.findViewById(R.id.et_user_first_name);
        etUserLastName = view.findViewById(R.id.et_user_last_name);
        etUserEmail = view.findViewById(R.id.et_user_email);
        etUserPhone = view.findViewById(R.id.et_user_phone);
        etUserPassword = view.findViewById(R.id.et_user_password);
        tvUserDisplayName = view.findViewById(R.id.tv_user_display_name);
        tvUserDisplayEmail = view.findViewById(R.id.tv_user_display_email);
        btnUpdateProfile = view.findViewById(R.id.btn_edit_profile);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
        adminBadge = view.findViewById(R.id.admin_badge);

        btnUpdateProfile.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_edit_profile) {
            updateUserProfile();
        } else if (id == R.id.btn_sign_out) {
            signOut();
        }
    }

    // קריאה מהשרת: הוספנו הגנה מפני נתונים ריקים (null) במידה והמשתמש התנתק בזמן הריצה
    private void showUserProfile() {
        if (currentUser == null) return;

        databaseService.getUser(currentUser.getId(), new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (!isAdded()) return;

                if (user == null) {
                    Toast.makeText(requireContext(), "Error: User not found", Toast.LENGTH_LONG).show();
                    signOut();
                    return;
                }

                currentUser = user;

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
                Log.e("ProfileFragment", "Error getting user profile", e);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Network error loading data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUserProfile() {
        String firstName = etUserFirstName.getText().toString();
        String lastName = etUserLastName.getText().toString();
        String phone = etUserPhone.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        if (!isValid(firstName, lastName, phone, email, password)) return;

        currentUser.setFirstname(firstName);
        currentUser.setLastname(lastName);
        currentUser.setPhone(phone);
        currentUser.setEmail(email);
        currentUser.setPassword(password);

        databaseService.updateUser(currentUser.getId(), new UnaryOperator<User>() {
            @Override
            public User apply(User user) {
                if (user != null) {
                    user.setFirstname(currentUser.getFirstname());
                    user.setLastname(currentUser.getLastname());
                    user.setPhone(currentUser.getPhone());
                    user.setEmail(currentUser.getEmail());
                    user.setPassword(currentUser.getPassword());
                }
                return user;
            }
        }, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                SharedPreferencesUtil.saveUser(requireContext(), currentUser);
                showUserProfile();
            }

            @Override
            public void onFailed(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
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

    // התנתקות: מנקה את השאריות מהזיכרון וסוגר את האקטיביטי הנוכחי בצורה בטוחה
    private void signOut() {
        SharedPreferencesUtil.signOutUser(requireContext());
        currentUser = null;

        Intent landingIntent = new Intent(requireActivity(), LandingActivity.class);
        landingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(landingIntent);
        requireActivity().finish();
    }
}