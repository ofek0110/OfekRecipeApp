package com.example.ofek.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RecipeReviewActivity extends AppCompatActivity {

    private Recipe recipe;
    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions;
    private TextInputEditText etAdminNotes;
    private Button btnApprove, btnReject;
    private DatabaseReference recipeRef;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_review);

        // קבלת המתכון מה-Intent
        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe == null) {
            Toast.makeText(this, "Error: Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes").child(recipe.getId());
        currentUser = SharedPreferencesUtil.getUser(this);

        initializeViews();
        displayRecipeData();
        setupViewForUserRole(); // התאמת התצוגה לפי סוג המשתמש

        btnApprove.setOnClickListener(v -> updateRecipeStatus(true));
        btnReject.setOnClickListener(v -> updateRecipeStatus(false));
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvReviewTitle);
        tvDescription = findViewById(R.id.tvReviewDescription);
        tvIngredients = findViewById(R.id.tvReviewIngredients);
        tvInstructions = findViewById(R.id.tvReviewInstructions);
        etAdminNotes = findViewById(R.id.etAdminNotes);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
    }

    private void displayRecipeData() {
        tvTitle.setText(recipe.getTitle());
        tvDescription.setText(recipe.getDescription());
        tvIngredients.setText(recipe.getIngredients());
        tvInstructions.setText(recipe.getInstructions());

        if (recipe.getAdminNotes() != null) {
            etAdminNotes.setText(recipe.getAdminNotes());
        }
    }

    // פונקציה חדשה: הסתרת כפתורים אם המשתמש אינו מנהל
    private void setupViewForUserRole() {
        if (currentUser == null || !currentUser.isAdmin()) {
            // משתמש רגיל - הסתרת כלי הניהול
            btnApprove.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            etAdminNotes.setVisibility(View.GONE);

            // שינוי כותרת אם צריך (אופציונלי)
            // setTitle("Recipe Details");
        } else {
            // מנהל - הצגת הכלים
            btnApprove.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
            etAdminNotes.setVisibility(View.VISIBLE);
        }
    }

    private void updateRecipeStatus(boolean approve) {
        if (currentUser == null || !currentUser.isAdmin()) return;

        String notes = etAdminNotes.getText().toString().trim();

        recipeRef.child("approved").setValue(approve);
        recipeRef.child("adminNotes").setValue(notes)
                .addOnSuccessListener(aVoid -> {
                    String msg = approve ? "Recipe Approved!" : "Recipe Rejected";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating status", Toast.LENGTH_SHORT).show();
                });
    }
}