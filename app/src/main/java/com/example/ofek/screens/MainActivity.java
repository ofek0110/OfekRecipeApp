package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList;
    private List<Recipe> filteredList;
    private DatabaseReference recipesRef;

    private EditText etSearch;
    private TextView btnUsers, btnRequests;
    private TextView tvRequestsBadge; // התגית האדומה
    private FloatingActionButton fabAddRecipe;
    private BottomNavigationView bottomNavigationView;

    private ImageView IvMyRecipes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LogIn.class));
            finish();
            return;
        }
        String currentUserId = currentUser.getId();

        rvRecipes = findViewById(R.id.RvRecipes);
        etSearch = findViewById(R.id.EtSearch);
        btnUsers = findViewById(R.id.BtnUsers);
        btnRequests = findViewById(R.id.BtnRequests);
        tvRequestsBadge = findViewById(R.id.TvRequestsBadge); // חיבור התגית האדומה
        fabAddRecipe = findViewById(R.id.FabAddRecipe);
        bottomNavigationView = findViewById(R.id.BottomNavigationView);
        IvMyRecipes = findViewById(R.id.IvMyRecipes);

        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        recipeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new RecipeAdapter(currentUserId, false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(MainActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {
            }
        });

        rvRecipes.setAdapter(adapter);

        IvMyRecipes.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyRecipesActivity.class));
        });

        fabAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddRecipeActivity.class));
        });

        btnUsers.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UsersList.class));
        });

        btnRequests.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecipeRequestsActivity.class));
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, UserProfile.class));
                return true;
            } else if (itemId == R.id.nav_explore) {
                return true;
            }
            return false;
        });

        loadRecipesFromFirebase();
    }

    private void filterRecipes(String text) {
        filteredList.clear();
        for (Recipe recipe : recipeList) {
            if (recipe.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(recipe);
            }
        }
        adapter.setRecipeList(filteredList);
    }

    private void loadRecipesFromFirebase() {
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                int pendingCount = 0; // מונה לבקשות הממתינות לאישור

                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    if (recipe != null) {
                        if (recipe.isApproved()) {
                            recipeList.add(recipe);
                        } else {
                            pendingCount++; // מגדיל את המונה אם המתכון לא מאושר
                        }
                    }
                }
                Collections.reverse(recipeList);

                filteredList.clear();
                filteredList.addAll(recipeList);
                adapter.setRecipeList(filteredList);

                // עדכון התגית האדומה על כפתור הבקשות
                if (pendingCount > 0) {
                    tvRequestsBadge.setVisibility(View.VISIBLE);
                    if (pendingCount > 99) {
                        tvRequestsBadge.setText("99+");
                    } else {
                        tvRequestsBadge.setText(String.valueOf(pendingCount));
                    }
                } else {
                    tvRequestsBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}