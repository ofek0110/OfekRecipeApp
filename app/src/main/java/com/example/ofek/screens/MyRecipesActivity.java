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

    private RecyclerView rvMyRecipes;
    private TextView tvEmptyState;
    private RecipeAdapter adapter;
    private List<Recipe> myRecipesList;
    private DatabaseReference recipesRef;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טוען את קובץ ה-XML
        setContentView(R.layout.activity_my_recipes);

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        // --- התיקון: שימוש ב-id עם אות קטנה (rvMyRecipes) ---
        rvMyRecipes = findViewById(R.id.RvMyRecipes);
        tvEmptyState = findViewById(R.id.TvEmptyState);

        rvMyRecipes.setLayoutManager(new LinearLayoutManager(this));

        myRecipesList = new ArrayList<>();

        adapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                handleRecipeClick(recipe);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }
        });

        rvMyRecipes.setAdapter(adapter);
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

                        // הופך את הרשימה כדי לראות את החדשים למעלה
                        Collections.reverse(myRecipesList);

                        // טיפול במצב שאין מתכונים
                        if (myRecipesList.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvMyRecipes.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            rvMyRecipes.setVisibility(View.VISIBLE);
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
            Toast.makeText(this, "Published! Good job.", Toast.LENGTH_SHORT).show();
        } else {
            // בדיקה אם יש הערת מנהל (דחייה)
            if (recipe.getAdminNotes() != null && !recipe.getAdminNotes().isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Action Required")
                        .setMessage("Admin rejected this recipe.\nReason: " + recipe.getAdminNotes() + "\n\nDo you want to fix it now?")
                        .setPositiveButton("Fix Now", (dialog, which) -> {
                            Intent intent = new Intent(MyRecipesActivity.this, AddRecipeActivity.class);
                            // מעביר את המתכון לעריכה
                            intent.putExtra("RECIPE_TO_EDIT", recipe);
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            } else {
                Toast.makeText(this, "Pending approval...", Toast.LENGTH_SHORT).show();
            }
        }
    }
}