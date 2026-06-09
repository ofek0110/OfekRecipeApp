package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.List;
import java.util.function.Predicate;

// מסך המציג לאדמין את כל בקשות המתכונים החדשות שממתינות לאישור שלו במערכת
public class RecipeRequestsActivity extends AppCompatActivity {

    private RecyclerView RvRequests;
    private RecipeAdapter adapter;
    private TextView TvPageTitle;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_requests);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // בדיקת אבטחה בסיסית: שליפת המשתמש וסגירת המסך אם אינו מחובר
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        // אתחול רכיבי ה-UI וקישורם ל-XML
        TvPageTitle = findViewById(R.id.tvPageTitle);
        RvRequests = findViewById(R.id.rvRecipeRequests);

        // הגדרת הרשימה (RecyclerView) וחיבור האדפטר עם מאזין לחיצה למעבר למסך הבדיקה
        RvRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(currentUser.getId(), false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(RecipeRequestsActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }

            @Override
            public void onFavoriteClick(Recipe recipe, boolean isFavorite) {}
        });
        RvRequests.setAdapter(adapter);
    }

    // רענון אוטומטי של רשימת הבקשות בכל פעם שחוזרים למסך זה (למשל לאחר בדיקת מתכון)
    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    // פנייה ל-Firebase: שליפת רשימת המתכונים הכללית וסינון שלה כך שישארו רק מתכונים במצב ממתין (Pending)
    private void loadRequests() {
        DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
            @Override
            public void onCompleted(List<Recipe> recipes) {
                recipes.removeIf(recipe -> !recipe.isPending());
                adapter.setRecipeList(recipes);
            }

            @Override
            public void onFailed(Exception e) {
                // טיפול בשגיאות תקשורת במידת הצורך
            }
        });
    }
}