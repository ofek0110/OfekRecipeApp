package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// אקטיביטי המציג למשתמש את רשימת כל המתכונים שהוא עצמו העלה לאפליקציה (כולל הסטטוס שלהם)
public class MyRecipesActivity extends AppCompatActivity {

    // הגדרת רכיבי ה-UI והמשתנים הגלובליים של המסך
    private RecyclerView RvMyRecipes;
    private TextView TvEmptyState;
    private RecipeAdapter adapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // בדיקת אבטחה בסיסית: אם אין משתמש מחובר, נסגור את המסך מיד
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        RvMyRecipes = findViewById(R.id.RvMyRecipes);
        TvEmptyState = findViewById(R.id.TvEmptyState);

        // אתחול רכיב הרשימה והגדרת האדפטר (במצב showStatus = true כדי להציג את סטטוס אישור האדמין)
        RvMyRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(currentUser.getId(), true, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                // ניתוב חכם של הלחיצה לפי מצב המתכון הנוכחי
                handleRecipeClick(recipe);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }

            @Override
            public void onFavoriteClick(Recipe recipe, boolean isFavorite) {}
        });
        RvMyRecipes.setAdapter(adapter);
    }

    // מנגנון רענון אוטומטי: נטען מחדש את המתכונים בכל פעם שהמשתמש חוזר למסך (למשל אחרי שחזר ממסך עריכה)
    @Override
    protected void onResume() {
        super.onResume();
        loadMyRecipes();
    }

    // שליפת כל המתכונים מ-Firebase וסינון שלהם כך שיופיעו רק המתכונים ששייכים ל-ID של המשתמש הנוכחי
    private void loadMyRecipes() {
        DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
            @Override
            public void onCompleted(List<Recipe> recipes) {
                // סינון הרשימה: משאירים רק מתכונים שיוצר המתכון שלהם הוא המשתמש הנוכחי
                recipes.removeIf(recipe -> !Objects.equals(recipe.getUserId(), currentUser.getId()));
                Collections.reverse(recipes); // הפיכת הרשימה כדי שהמתכון האחרון שהועלה יופיע ראשון


                if (recipes.isEmpty()) {
                    TvEmptyState.setVisibility(View.VISIBLE);
                    RvMyRecipes.setVisibility(View.GONE);
                } else {
                    TvEmptyState.setVisibility(View.GONE);
                    RvMyRecipes.setVisibility(View.VISIBLE);
                }


                adapter.setRecipeList(recipes);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MyRecipesActivity.this, "Error loading recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // לוגיקת לחיצה על מתכון: בודק את הסטטוס ומציג דיאלוגים מותאמים אישית (מתכון מאושר, ממתין, או נדחה לתיקון)
    private void handleRecipeClick(Recipe recipe) {
        if (recipe.isApproved()) {

            showRecipeOptionsDialog(recipe, "This recipe is live!\nNote: If you edit it, it will return to pending status and require admin approval again.");
        } else if (recipe.getAdminNotes() != null && !recipe.getAdminNotes().isEmpty()) {
            // אם המתכון נדחה לתיקון, נציג דיאלוג מיוחד שמציג את סיבת הדחייה של האדמין וכפתור למעבר מהיר לעריכה
            new AlertDialog.Builder(this)
                    .setTitle("Action Required")
                    .setMessage("Admin rejected this recipe.\nReason: " + recipe.getAdminNotes() + "\n\nWhat would you like to do?")
                    .setPositiveButton("Fix Now", (dialog, which) -> {
                        Intent intent = new Intent(MyRecipesActivity.this, AddRecipeActivity.class);
                        intent.putExtra("RECIPE_ID_TO_EDIT", recipe.getId());
                        startActivity(intent);
                    })
                    .setNeutralButton("View Recipe", (dialog, which) -> {
                        Intent intent = new Intent(MyRecipesActivity.this, RecipeReviewActivity.class);
                        intent.putExtra("recipe_id", recipe.getId());
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        } else {
            // אם המתכון עדיין ממתין לבדיקה ראשונית
            showRecipeOptionsDialog(recipe, "This recipe is currently waiting for admin approval. You can still view or edit it.");
        }
    }

    // יצירת דיאלוג אפשרויות סטנדרטי (AlertDialog) המאפשר למשתמש לבחור בין צפייה במתכון לבין מעבר למסך העריכה שלו
    private void showRecipeOptionsDialog(Recipe recipe, String message) {
        new AlertDialog.Builder(this)
                .setTitle(recipe.getTitle())
                .setMessage(message)
                .setPositiveButton("View", (dialog, which) -> {
                    Intent intent = new Intent(MyRecipesActivity.this, RecipeReviewActivity.class);
                    intent.putExtra("recipe_id", recipe.getId());
                    startActivity(intent);
                })
                .setNegativeButton("Edit", (dialog, which) -> {
                    Intent intent = new Intent(MyRecipesActivity.this, AddRecipeActivity.class);
                    intent.putExtra("RECIPE_ID_TO_EDIT", recipe.getId());
                    startActivity(intent);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}