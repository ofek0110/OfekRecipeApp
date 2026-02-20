package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRecipesActivity extends AppCompatActivity {

    private RecyclerView RvMyRecipes;
    private TextView TvEmptyState;
    private RecipeAdapter adapter;
    private List<Recipe> myRecipesList;
    private DatabaseReference recipesRef;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        RvMyRecipes = findViewById(R.id.RvMyRecipes);
        TvEmptyState = findViewById(R.id.TvEmptyState);

        RvMyRecipes.setLayoutManager(new LinearLayoutManager(this));

        myRecipesList = new ArrayList<>();

        adapter = new RecipeAdapter(currentUser.getId(), true, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                handleRecipeClick(recipe);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }
        });

        RvMyRecipes.setAdapter(adapter);
        loadMyRecipes();
    }

    private void loadMyRecipes() {
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        recipesRef.orderByChild("userId").equalTo(currentUser.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myRecipesList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Recipe recipe = data.getValue(Recipe.class);
                            if (recipe != null) {
                                myRecipesList.add(recipe);
                            }
                        }

                        Collections.reverse(myRecipesList);

                        if (myRecipesList.isEmpty()) {
                            TvEmptyState.setVisibility(View.VISIBLE);
                            RvMyRecipes.setVisibility(View.GONE);
                        } else {
                            TvEmptyState.setVisibility(View.GONE);
                            RvMyRecipes.setVisibility(View.VISIBLE);
                        }

                        adapter.setRecipeList(myRecipesList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyRecipesActivity.this, "Error loading recipes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleRecipeClick(Recipe recipe) {
        if (recipe.isApproved()) {
            showRecipeOptionsDialog(recipe, "This recipe is live!\nNote: If you edit it, it will return to pending status and require admin approval again.");
        } else if (recipe.getAdminNotes() != null && !recipe.getAdminNotes().isEmpty()) {
            // מתכון שנדחה - מציגים את סיבת הדחייה ונותנים אפשרות לצפות או לתקן
            new AlertDialog.Builder(this)
                    .setTitle("Action Required")
                    .setMessage("Admin rejected this recipe.\nReason: " + recipe.getAdminNotes() + "\n\nWhat would you like to do?")
                    .setPositiveButton("Fix Now", (dialog, which) -> {
                        Intent intent = new Intent(MyRecipesActivity.this, AddRecipeActivity.class);
                        intent.putExtra("RECIPE_TO_EDIT", recipe);
                        startActivity(intent);
                    })
                    .setNeutralButton("View Recipe", (dialog, which) -> {
                        Intent intent = new Intent(MyRecipesActivity.this, RecipeReviewActivity.class);
                        intent.putExtra("recipe", recipe);
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        } else {
            showRecipeOptionsDialog(recipe, "This recipe is currently waiting for admin approval. You can still view or edit it.");
        }
    }

    // פונקציית עזר שחוסכת קוד כפול - מקפיצה את הדיאלוג של בחירה בין צפייה לעריכה
    private void showRecipeOptionsDialog(Recipe recipe, String message) {
        new AlertDialog.Builder(this)
                .setTitle(recipe.getTitle())
                .setMessage(message)
                .setPositiveButton("View", (dialog, which) -> {
                    Intent intent = new Intent(MyRecipesActivity.this, RecipeReviewActivity.class);
                    intent.putExtra("recipe", recipe);
                    startActivity(intent);
                })
                .setNegativeButton("Edit", (dialog, which) -> {
                    Intent intent = new Intent(MyRecipesActivity.this, AddRecipeActivity.class);
                    intent.putExtra("RECIPE_TO_EDIT", recipe);
                    startActivity(intent);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}