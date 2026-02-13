package com.example.ofek.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RecipeReviewActivity extends AppCompatActivity {

    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions, tvTime, tvDifficulty;
    private Button btnApprove, btnReject;
    private View adminPanel; // משתנה לפאנל הניהול
    private Recipe currentRecipe;
    private DatabaseReference recipesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_review);

        if (getIntent().hasExtra("recipe")) {
            currentRecipe = (Recipe) getIntent().getSerializableExtra("recipe");
        } else {
            Toast.makeText(this, "Error: No recipe data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        initializeViews();
        displayRecipeData();
        setupClickListeners();
    }

    private void initializeViews() {
        // חיבור לרכיבים לפי ה-IDs המדויקים מה-XML שלך
        tvTitle = findViewById(R.id.tvReviewTitle);
        tvDescription = findViewById(R.id.tvReviewDescription); // תוקן מ-tvReviewDesc
        tvIngredients = findViewById(R.id.tvReviewIngredients);
        tvInstructions = findViewById(R.id.tvReviewInstructions);
        tvTime = findViewById(R.id.tvReviewTime);         // הוספתי
        tvDifficulty = findViewById(R.id.tvReviewDifficulty); // הוספתי

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);

        // חיבור לפאנל הניהול כדי להציג אותו
        adminPanel = findViewById(R.id.adminPanel);
        if (adminPanel != null) {
            adminPanel.setVisibility(View.VISIBLE); // הופך את הכפתורים לנראים
        }
    }

    private void displayRecipeData() {
        if (currentRecipe != null) {
            tvTitle.setText(currentRecipe.getTitle());
            tvDescription.setText(currentRecipe.getDescription());
            tvIngredients.setText(currentRecipe.getIngredients());
            tvInstructions.setText(currentRecipe.getInstructions());

            // הצגת זמן וקושי (עם אימוג'ים כמו ב-XML)
            tvTime.setText("🕒 " + currentRecipe.getPreparationTime());
            tvDifficulty.setText("🔥 " + currentRecipe.getDifficulty());
        }
    }

    private void setupClickListeners() {
        btnApprove.setOnClickListener(v -> approveRecipe());
        btnReject.setOnClickListener(v -> showRejectDialog());
    }

    private void approveRecipe() {
        if (currentRecipe == null) return;

        currentRecipe.setApproved(true);
        currentRecipe.setAdminNotes(""); // ניקוי הערות אם היו

        recipesRef.child(currentRecipe.getId()).setValue(currentRecipe)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Recipe Approved Successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // חוזר למסך הקודם
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error approving recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showRejectDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter reason for rejection...");

        // הוספת ריווח קטן לתיבת הטקסט בדיאלוג
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reject Recipe")
                .setMessage("Please explain why this recipe is being rejected so the user can fix it:")
                .setView(input)
                .setPositiveButton("Reject & Send Back", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        rejectRecipe(reason);
                    } else {
                        Toast.makeText(this, "Rejection reason is required!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rejectRecipe(String reason) {
        if (currentRecipe == null) return;

        currentRecipe.setApproved(false);
        currentRecipe.setAdminNotes(reason); // שמירת סיבת הדחייה

        recipesRef.child(currentRecipe.getId()).setValue(currentRecipe)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Recipe rejected and sent back to user.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error rejecting recipe", Toast.LENGTH_SHORT).show()
                );
    }
}