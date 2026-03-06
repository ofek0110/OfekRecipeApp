package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedRecipesActivity extends AppCompatActivity {

    private RecyclerView rvSavedRecipes;
    private TextView tvEmptySavedState;
    private BottomNavigationView bottomNavigationView;
    private RecipeAdapter adapter;
    private List<Recipe> savedRecipesList;
    private DatabaseReference favoritesRef;
    private DatabaseReference recipesRef;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_recipes);

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        rvSavedRecipes = findViewById(R.id.RvSavedRecipes);
        tvEmptySavedState = findViewById(R.id.TvEmptySavedState);
        bottomNavigationView = findViewById(R.id.BottomNavigationViewSaved);

        rvSavedRecipes.setLayoutManager(new LinearLayoutManager(this));
        savedRecipesList = new ArrayList<>();

        adapter = new RecipeAdapter(currentUser.getId(), false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(SavedRecipesActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }
        });

        rvSavedRecipes.setAdapter(adapter);

        bottomNavigationView.setSelectedItemId(R.id.nav_saved);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_saved) {
                return true;
            } else if (itemId == R.id.nav_home) {
                startActivity(new Intent(SavedRecipesActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(SavedRecipesActivity.this, UserProfile.class));
                finish();
                return true;
            }
            return false;
        });

        loadSavedRecipes();
    }

    private void loadSavedRecipes() {
        favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(currentUser.getId());
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot favoritesSnapshot) {
                savedRecipesList.clear();

                if (!favoritesSnapshot.exists() || !favoritesSnapshot.hasChildren()) {
                    updateUI(true);
                    return;
                }

                for (DataSnapshot favData : favoritesSnapshot.getChildren()) {
                    String recipeId = favData.getKey();
                    if (recipeId != null) {
                        recipesRef.child(recipeId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot recipeSnapshot) {
                                Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                                if (recipe != null) {
                                    savedRecipesList.add(recipe);
                                }

                                Collections.reverse(savedRecipesList);
                                adapter.setRecipeList(savedRecipesList);
                                updateUI(savedRecipesList.isEmpty());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SavedRecipesActivity.this, "Error loading saved recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            tvEmptySavedState.setVisibility(View.VISIBLE);
            rvSavedRecipes.setVisibility(View.GONE);
        } else {
            tvEmptySavedState.setVisibility(View.GONE);
            rvSavedRecipes.setVisibility(View.VISIBLE);
        }
    }
}