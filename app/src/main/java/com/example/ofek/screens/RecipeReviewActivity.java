package com.example.ofek.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class RecipeReviewActivity extends AppCompatActivity {

    private TextView TvTitle, TvDescription, TvIngredients, TvInstructions, TvTime, TvDifficulty;
    private Button BtnApprove, BtnReject, BtnRemove;
    private View LayoutPendingButtons;
    private ImageView IvRecipeImage;
    private TextInputEditText EtAdminNotes;
    private View AdminPanel;

    // משתני הדירוג
    private RatingBar RbRecipeRatingDisplay, RbUserRating;
    private TextView TvRatingCount, TvUserRatingTitle;
    private MaterialCardView CardUserRating;

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
        loadRecipeData();
    }

    private void loadRecipeData() {
        DatabaseService.getInstance().getRecipe(recipeId, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe recipe) {
                if (recipe != null) {
                    currentRecipe = recipe;
                    displayRecipeData();
                    setupAdminPanel();
                    setupClickListeners();
                } else {
                    Toast.makeText(RecipeReviewActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error loading recipe", Toast.LENGTH_SHORT).show();
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
        IvRecipeImage = findViewById(R.id.IvRecipeImage);

        // חיבור רכיבי הדירוג
        RbRecipeRatingDisplay = findViewById(R.id.RbRecipeRatingDisplay);
        TvRatingCount = findViewById(R.id.TvRatingCount);
        RbUserRating = findViewById(R.id.RbUserRating);
        CardUserRating = findViewById(R.id.CardUserRating);
        TvUserRatingTitle = findViewById(R.id.TvUserRatingTitle);

        AdminPanel.setVisibility(View.GONE);
    }

    private void displayRecipeData() {
        TvTitle.setText(currentRecipe.getTitle());
        TvDescription.setText(currentRecipe.getDescription());
        TvIngredients.setText(currentRecipe.getIngredients());
        TvInstructions.setText(currentRecipe.getInstructions());

        TvTime.setText("🕒 " + currentRecipe.getPreparationTime());
        TvDifficulty.setText("🔥 " + currentRecipe.getDifficulty());

        // מציג את הממוצע ואת כמות המדרגים למעלה
        RbRecipeRatingDisplay.setRating(currentRecipe.getRating());
        TvRatingCount.setText("(" + currentRecipe.getNumRatings() + " ratings)");

        if (currentRecipe.getImageBase64() != null && !currentRecipe.getImageBase64().isEmpty()) {
            IvRecipeImage.setImageBitmap(ImageUtil.convertFrom64base(currentRecipe.getImageBase64()));
        }

        // --- לוגיקת הצגת כרטיסיית הדירוג למשתמש ---
        if (!currentRecipe.isApproved()) {
            // אם המתכון לא מאושר - מסתירים את אפשרות ההצבעה לחלוטין מכולם
            CardUserRating.setVisibility(View.GONE);
        } else {
            CardUserRating.setVisibility(View.VISIBLE);

            // בדיקה אם המשתמש כבר דירג בעבר
            if (currentRecipe.getRaters() != null && currentRecipe.getRaters().containsKey(currentUser.getId())) {
                TvUserRatingTitle.setText("Your Rating:");
                RbUserRating.setRating(currentRecipe.getRaters().get(currentUser.getId()));
                RbUserRating.setIsIndicator(true); // נועל את המד כדי שלא ישנו
            } else {
                TvUserRatingTitle.setText("Rate this recipe:");
                RbUserRating.setRating(0);
                RbUserRating.setIsIndicator(false); // מאפשר בחירה
            }
        }
    }

    private void setupAdminPanel() {
        if (currentRecipe == null) return;

        if (currentUser == null || !currentUser.isAdmin()) {
            AdminPanel.setVisibility(View.GONE);
            return;
        }

        if (currentRecipe.isApproved()) {
            AdminPanel.setVisibility(View.VISIBLE);
            LayoutPendingButtons.setVisibility(View.GONE);
            BtnRemove.setVisibility(View.VISIBLE);

        } else if (currentRecipe.getAdminNotes() != null && !currentRecipe.getAdminNotes().trim().isEmpty()) {
            AdminPanel.setVisibility(View.GONE);

        } else {
            AdminPanel.setVisibility(View.VISIBLE);
            LayoutPendingButtons.setVisibility(View.VISIBLE);
            BtnRemove.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        BtnApprove.setOnClickListener(v -> approveRecipe());
        BtnReject.setOnClickListener(v -> handleRejectClick());
        BtnRemove.setOnClickListener(v -> showDeleteConfirmationDialog());

        RbUserRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                submitRecipeRating(rating);
            }
        });
    }

    private void submitRecipeRating(float newRating) {
        if (currentRecipe == null) return;
        RbUserRating.setIsIndicator(true); // נועל מיד למניעת לחיצות כפולות

        DatabaseService.getInstance().updateRecipes(currentRecipe.getId(), new UnaryOperator<Recipe>() {
            @Override
            public Recipe apply(Recipe recipe) {
                if (recipe != null) {
                    Map<String, Float> raters = recipe.getRaters();
                    if (raters == null) {
                        raters = new HashMap<>();
                    }

                    // מוודאים שוב מול השרת שהמשתמש באמת לא דירג
                    if (!raters.containsKey(currentUser.getId())) {
                        raters.put(currentUser.getId(), newRating);
                        recipe.setRaters(raters);
                        recipe.setNumRatings(raters.size());

                        // חישוב ממוצע חדש ומדויק
                        float sum = 0;
                        for (Float r : raters.values()) {
                            sum += r;
                        }
                        recipe.setRating(sum / raters.size());
                    }
                }
                return recipe;
            }
        }, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe serverRecipe) {
                if(serverRecipe != null) {
                    Toast.makeText(RecipeReviewActivity.this, "Thank you for rating!", Toast.LENGTH_SHORT).show();
                    // עדכון התצוגה המקומית מיד לאחר השמירה בשרת
                    currentRecipe = serverRecipe;
                    RbRecipeRatingDisplay.setRating(currentRecipe.getRating());
                    TvRatingCount.setText("(" + currentRecipe.getNumRatings() + " ratings)");
                    TvUserRatingTitle.setText("Your Rating:");
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error saving rating", Toast.LENGTH_SHORT).show();
                RbUserRating.setIsIndicator(false); // משחרר את הנעילה במקרה של שגיאה
            }
        });
    }

    private void approveRecipe() {
        if (currentRecipe == null) return;
        currentRecipe.setApproved(true);
        currentRecipe.setAdminNotes("");

        DatabaseService.getInstance().updateRecipes(currentRecipe.getId(), recipe -> {
            if (recipe != null) {
                recipe.setApproved(currentRecipe.isApproved());
                recipe.setAdminNotes(currentRecipe.getAdminNotes());
            }
            return recipe;
        }, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe serverRecipe) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe Approved!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error approving recipe", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRejectClick() {
        String reason = EtAdminNotes.getText().toString().trim();
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }
        rejectRecipe(reason);
    }

    private void rejectRecipe(String reason) {
        if (currentRecipe == null) return;
        currentRecipe.setApproved(false);
        currentRecipe.setAdminNotes(reason);

        DatabaseService.getInstance().updateRecipes(currentRecipe.getId(), recipe -> {
            if (recipe != null) {
                recipe.setApproved(currentRecipe.isApproved());
                recipe.setAdminNotes(currentRecipe.getAdminNotes());
            }
            return recipe;
        }, new DatabaseService.DatabaseCallback<Recipe>() {
            @Override
            public void onCompleted(@Nullable Recipe serverRecipe) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe returned.", Toast.LENGTH_LONG).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error returning recipe", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to permanently delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipeFromFirebase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipeFromFirebase() {
        if (currentRecipe == null) return;
        DatabaseService.getInstance().deleteRecipe(currentRecipe.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(@Nullable Void v) {
                Toast.makeText(RecipeReviewActivity.this, "Recipe deleted.", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(RecipeReviewActivity.this, "Error deleting recipe", Toast.LENGTH_SHORT).show();
            }
        });
    }
}