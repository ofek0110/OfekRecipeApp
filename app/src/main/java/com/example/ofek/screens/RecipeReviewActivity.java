package com.example.ofek.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.ImageUtil;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.function.UnaryOperator;

public class RecipeReviewActivity extends AppCompatActivity {

    private TextView TvTitle, TvDescription, TvIngredients, TvInstructions, TvTime, TvDifficulty;
    private Button BtnApprove, BtnReject, BtnRemove;
    private View LayoutPendingButtons;
    private ImageView imageView;
    private TextInputEditText EtAdminNotes;
    private View AdminPanel;
    private Recipe currentRecipe;
    private String recipeId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_review);

        currentUser = SharedPreferencesUtil.getUser(this);

        if (getIntent().hasExtra("recipe_id")) {
            recipeId = getIntent().getStringExtra("recipe_id");
        } else {
            Toast.makeText(this, "Error: No recipe data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        DatabaseService.getInstance().getRecipe(recipeId, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe recipe) {
                currentRecipe = recipe;
                displayRecipeData();
                setupAdminPanel();
                setupClickListeners();
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
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
        imageView = findViewById(R.id.recipe_review_view);

        if (currentUser.isAdmin()) {
            // הפאנל יוצג תמיד כדי שהמנהל יוכל לבדוק
            AdminPanel.setVisibility(View.VISIBLE);
        }
    }

    private void displayRecipeData() {
        TvTitle.setText(currentRecipe.getTitle());
        TvDescription.setText(currentRecipe.getDescription());
        TvIngredients.setText(currentRecipe.getIngredients());
        TvInstructions.setText(currentRecipe.getInstructions());

        TvTime.setText("🕒 " + currentRecipe.getPreparationTime());
        TvDifficulty.setText("🔥 " + currentRecipe.getDifficulty());
        if (currentRecipe.getImageBase64() != null)
            imageView.setImageBitmap(ImageUtil.convertFrom64base(currentRecipe.getImageBase64()));
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

        // התיקון כאן: מפעיל חלונית ששואלת האם למחוק את המתכון לצמיתות
        BtnRemove.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void approveRecipe() {
        if (currentRecipe == null) return;

        currentRecipe.setApproved(true);
        currentRecipe.setAdminNotes("");

        DatabaseService.getInstance().updateRecipes(currentRecipe.getId(), new UnaryOperator<Recipe>() {
            @Override
            public Recipe apply(Recipe recipe) {
                if (recipe != null) {
                    recipe.setApproved(currentRecipe.isApproved());
                    recipe.setAdminNotes(currentRecipe.getAdminNotes());
                }
                return recipe;
            }
        }, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe serverRecipe) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe Approved Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error approving recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

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

        DatabaseService.getInstance().updateRecipes(currentRecipe.getId(), new UnaryOperator<Recipe>() {
            @Override
            public Recipe apply(Recipe recipe) {
                if (recipe != null) {
                    recipe.setApproved(currentRecipe.isApproved());
                    recipe.setAdminNotes(currentRecipe.getAdminNotes());
                }
                return recipe;
            }
        }, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe serverRecipe) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe returned to user.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error returning recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    // פונקציה חדשה: מציגה חלונית אישור לפני המחיקה
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to permanently delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipeFromFirebase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // פונקציה חדשה: מוחקת את המתכון מ-Firebase
    private void deleteRecipeFromFirebase() {
        if (currentRecipe == null) return;

        DatabaseService.getInstance().deleteRecipe(currentRecipe.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(@Nullable Void v) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe deleted permanently.", Toast.LENGTH_SHORT).show();
                finish(); // סוגר את המסך וחוזר למסך הקודם
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error deleting recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }
}