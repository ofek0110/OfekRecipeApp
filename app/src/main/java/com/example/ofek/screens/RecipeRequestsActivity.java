package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class RecipeRequestsActivity extends AppCompatActivity {

    private RecyclerView RvRequests;
    private RecipeAdapter adapter;
    private TextView TvPageTitle;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_requests);

        // שליפת המשתמש הנוכחי מה-SharedPreferences
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        // שימוש ב-IDs עם אותיות גדולות
        TvPageTitle = findViewById(R.id.tvPageTitle);
        RvRequests = findViewById(R.id.rvRecipeRequests);

        RvRequests.setLayoutManager(new LinearLayoutManager(this));

        // התיקון כאן: הוספנו את false בתור פרמטר שני
        adapter = new RecipeAdapter(currentUser.getId(), false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(RecipeRequestsActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }
        });

        RvRequests.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {

        DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
            @Override
            public void onCompleted(List<Recipe> recipes) {
                recipes.removeIf(recipe -> recipe.isApproved());
                recipes.removeIf(recipe -> !(recipe.getAdminNotes() == null || recipe.getAdminNotes().isEmpty()));
                adapter.setRecipeList(recipes);
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }
}