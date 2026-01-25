package com.example.ofek.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private User user;
    private TextView tvName;
    private Button btnLogout, btnShowProfile, btnAdminManageUsers, btnAdminAddRecipe;
    private ImageView ivArrow;
    private LinearLayout userHeader, menuOptions;
    private CardView adminPanelContainer;
    
    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private ExtendedFloatingActionButton fabCreateRecipe;
    private SearchView searchView;
    private ChipGroup chipGroupFilters;

    private String currentCategoryFilter = "All";
    private final List<String> categories = Arrays.asList("All", "Meat", "Dairy", "Vegan", "Dessert", "Salad", "Pasta");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupUserDetails();
        setupRecipeList();
        setupClickListeners();
        setupSearch();
        setupCategoryFilters();
        loadRecipes();
    }

    private void initializeViews() {
        tvName = findViewById(R.id.TvName);
        btnLogout = findViewById(R.id.LogOutBtn);
        btnShowProfile = findViewById(R.id.ShowProfileBtn);
        ivArrow = findViewById(R.id.ivArrow);
        userHeader = findViewById(R.id.userHeader); 
        menuOptions = findViewById(R.id.menuOptions);
        adminPanelContainer = findViewById(R.id.adminPanelContainer);
        btnAdminManageUsers = findViewById(R.id.btnAdminManageUsers);
        btnAdminAddRecipe = findViewById(R.id.btnAdminAddRecipe);
        
        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes);
        fabCreateRecipe = findViewById(R.id.fabCreateRecipe);
        searchView = findViewById(R.id.searchView);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
    }

    private void setupCategoryFilters() {
        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);
            
            if (category.equals("All")) {
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentCategoryFilter = category;
                    applyFilters();
                }
            });
            chipGroupFilters.addView(chip);
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });
    }

    private void applyFilters() {
        String query = searchView.getQuery().toString().toLowerCase();
        List<Recipe> filteredList = new ArrayList<>();

        for (Recipe recipe : recipeList) {
            boolean matchesSearch = recipe.getTitle().toLowerCase().contains(query);
            boolean matchesCategory = currentCategoryFilter.equals("All") || 
                                     (recipe.getCategory() != null && recipe.getCategory().equalsIgnoreCase(currentCategoryFilter));

            if (matchesSearch && matchesCategory) {
                filteredList.add(recipe);
            }
        }
        recipeAdapter.setRecipeList(filteredList);
    }

    private void setupRecipeList() {
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(MainActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {
                if (user != null && user.isAdmin()) {
                    showAdminRecipeDialog(recipe);
                }
            }
        });
        
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void showAdminRecipeDialog(Recipe recipe) {
        String[] options = {"Send for Correction", "Delete Recipe"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Recipe: " + recipe.getTitle());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showCorrectionDialog(recipe);
            } else if (which == 1) {
                deleteRecipe(recipe);
            }
        });
        builder.show();
    }

    private void showCorrectionDialog(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Notes for Correction");
        
        final EditText input = new EditText(this);
        input.setHint("What needs to be fixed?");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String notes = input.getText().toString();
            recipe.setApproved(false);
            recipe.setAdminNotes(notes);
            updateRecipeInFirebase(recipe, "Recipe sent back for correction");
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteRecipe(Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("recipes")
                            .child(recipe.getId())
                            .removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Recipe deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void updateRecipeInFirebase(Recipe recipe, String message) {
        FirebaseDatabase.getInstance().getReference("recipes")
                .child(recipe.getId())
                .setValue(recipe)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void loadRecipes() {
        DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    if (recipe != null && recipe.isApproved()) {
                        recipeList.add(recipe);
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUserDetails() {
        user = SharedPreferencesUtil.getUser(MainActivity.this);
        if (user != null) {
            tvName.setText(user.getFirstname());
            if (user.isAdmin()) {
                adminPanelContainer.setVisibility(View.VISIBLE);
            } else {
                adminPanelContainer.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(MainActivity.this, LandingActivity.class);
            startActivity(intent);
            finish();
        });

        btnShowProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfile.class));
        });

        btnAdminManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, UsersList.class));
        });

        btnAdminAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecipeRequestsActivity.class));
        });

        fabCreateRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        final boolean[] isOpen = {false};
        menuOptions.setVisibility(View.GONE);

        userHeader.setOnClickListener(v -> {
            if (isOpen[0]) {
                menuOptions.setVisibility(View.GONE);
                ivArrow.setRotation(0); 
            } else {
                menuOptions.setVisibility(View.VISIBLE);
                ivArrow.setRotation(180); 
            }
            isOpen[0] = !isOpen[0];
        });
    }
}
