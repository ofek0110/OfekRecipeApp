package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;

public class MainActivity extends AppCompatActivity {

    // שלב 1: הגדר את כל משתני הממשק כחברים במחלקה (Class Members)
    private User user;
    private TextView tvName;
    private Button btnLogout, btnShowProfile, btnAdminManageUsers, btnAdminAddRecipe;
    private ImageView ivArrow;
    private LinearLayout userHeader, menuOptions;
    private CardView adminPanelContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // שלב 2: קשר את המשתנים לרכיבים מה-XML במקום אחד
        initializeViews();

        // שלב 3: הפעל את הלוגיקה והגדר את המאזינים (Listeners)
        setupUserDetails();
        setupClickListeners();
    }

    /**
     * מתודה המאתחלת את כל רכיבי הממשק ומקשרת אותם למשתנים.
     */
    private void initializeViews() {
        tvName = findViewById(R.id.TvName);
        btnLogout = findViewById(R.id.LogOutBtn);
        btnShowProfile = findViewById(R.id.ShowProfileBtn);
        ivArrow = findViewById(R.id.ivArrow);
        userHeader = findViewById(R.id.userHeader); 
        menuOptions = findViewById(R.id.menuOptions);
        adminPanelContainer = findViewById(R.id.adminPanelContainer);
        btnAdminManageUsers = findViewById(R.id.btnAdminManageUsers);
        btnAdminAddRecipe = findViewById(R.id.btnAdminAddRecipe);
    }

    /**
     * מתודה המאחזרת את פרטי המשתמש, מציגה את שמו, וכן מציגה את פאנל הניהול אם המשתמש הוא מנהל.
     */
    private void setupUserDetails() {
        user = SharedPreferencesUtil.getUser(MainActivity.this);
        if (user != null) {
            tvName.setText(user.getFirstname());
            if (user.isAdmin()) {
                adminPanelContainer.setVisibility(View.VISIBLE);
            } else {
                adminPanelContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * מתודה המגדירה את כל מאזיני הלחיצה (Click Listeners).
     */
    private void setupClickListeners() {
        if (userHeader == null) {
            return; 
        }

        btnLogout.setOnClickListener(v -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(MainActivity.this, LandingActivity.class);
            startActivity(intent);
            finish();
        });

        btnShowProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfile.class));
        });

        btnAdminManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, UsersList.class));
        });

        btnAdminAddRecipe.setOnClickListener(v -> {
            // Add your logic for recipe requests here
        });

        final boolean[] isOpen = {false};
        menuOptions.setVisibility(View.GONE);

        userHeader.setOnClickListener(v -> {
            if (isOpen[0]) {
                menuOptions.setVisibility(View.GONE);
                ivArrow.setRotation(0); 
            } else {
                menuOptions.setVisibility(View.VISIBLE);
                ivArrow.setRotation(180); 
            }
            isOpen[0] = !isOpen[0];
        });
    }
}
