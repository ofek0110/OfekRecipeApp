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

    private TextView TvTitle, TvDescription, TvIngredients, TvInstructions, TvTime, TvDifficulty;
    private Button BtnApprove, BtnReject, BtnRemove;
    private View LayoutPendingButtons;
    private TextInputEditText EtAdminNotes;
    private View AdminPanel;
    private Recipe currentRecipe;
    private DatabaseReference recipesRef;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_review);

        currentUser = SharedPreferencesUtil.getUser(this);

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
        setupAdminPanel();
    }

    private void initializeViews() {
        TvTitle = findViewById(R.id.TvReviewTitle);
        TvDescription = findViewById(R.id.TvReviewDescription);
        TvIngredients = findViewById(R.id.TvReviewIngredients);
        TvInstructions = findViewById(R.id.TvReviewInstructions);
        TvTime = findViewById(R.id.TvReviewTime);
        TvDifficulty = findViewById(R.id.TvReviewDifficulty);

        BtnApprove = findViewById(R.id.BtnApprove);
        BtnReject = findViewById(R.id.BtnReject);
        BtnRemove = findViewById(R.id.BtnRemove);
        LayoutPendingButtons = findViewById(R.id.LayoutPendingButtons);
        EtAdminNotes = findViewById(R.id.EtAdminNotes);
        AdminPanel = findViewById(R.id.AdminPanel);

        if (AdminPanel != null) {
            // הסרתי את הבדיקה אם המשתמש הוא מנהל - הפאנל יוצג תמיד כדי שתוכל לבדוק
            AdminPanel.setVisibility(View.VISIBLE);
        }
    }

    private void displayRecipeData() {
        if (currentRecipe != null) {
            TvTitle.setText(currentRecipe.getTitle());
            TvDescription.setText(currentRecipe.getDescription());
            TvIngredients.setText(currentRecipe.getIngredients());
            TvInstructions.setText(currentRecipe.getInstructions());

            TvTime.setText("🕒 " + currentRecipe.getPreparationTime());
            TvDifficulty.setText("🔥 " + currentRecipe.getDifficulty());
        }
    }

    private void setupAdminPanel() {
        if (currentRecipe == null) return;

        if (currentRecipe.isApproved()) {
            // המתכון כבר מאושר: מסתירים את שורת Approve/Reject, מציגים את כפתור ההסרה הגדול
            LayoutPendingButtons.setVisibility(View.GONE);
            BtnRemove.setVisibility(View.VISIBLE);
        } else {
            // המתכון ממתין לאישור: מציגים את Approve/Reject, מסתירים את ההסרה
            LayoutPendingButtons.setVisibility(View.VISIBLE);
            BtnRemove.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        BtnApprove.setOnClickListener(v -> approveRecipe());
        BtnReject.setOnClickListener(v -> handleRejectClick());

        // גם כפתור ה-Remove מפעיל את פונקציית הדחייה כדי להחזיר את המתכון למשתמש
        BtnRemove.setOnClickListener(v -> handleRejectClick());
    }

    private void approveRecipe() {
        if (currentRecipe == null) return;

        currentRecipe.setApproved(true);
        currentRecipe.setAdminNotes("");

        recipesRef.child(currentRecipe.getId()).setValue(currentRecipe)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Recipe Approved Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error approving recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void handleRejectClick() {
        String reason = EtAdminNotes.getText().toString().trim();

        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason in the notes field", Toast.LENGTH_SHORT).show();
            return;
        }

        rejectRecipe(reason);
    }

    private void rejectRecipe(String reason) {
        if (currentRecipe == null) return;

        currentRecipe.setApproved(false);
        currentRecipe.setAdminNotes(reason);

        recipesRef.child(currentRecipe.getId()).setValue(currentRecipe)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Recipe returned to user.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error returning recipe", Toast.LENGTH_SHORT).show()
                );
    }
}