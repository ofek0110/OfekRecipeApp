package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private User user;
    private TextView tvName;
    private Button btnLogout, btnShowProfile, btnAdminManageUsers, btnAdminAddRecipe;
    private ImageView ivArrow;
    private LinearLayout userHeader, menuOptions;
    private CardView adminPanelContainer;
    
    // רכיבים חדשים עבור הרשימה והכפתור
    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private ExtendedFloatingActionButton fabCreateRecipe;

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
        
        // אתחול רכיבים חדשים
        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes);
        fabCreateRecipe = findViewById(R.id.fabCreateRecipe);
    }

    private void setupRecipeList() {
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                // Logic for clicking a recipe
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {
                // Logic for long click
            }
        });
        
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void loadRecipes() {
        DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    // מציגים רק מתכונים מאושרים למשתמש רגיל
                    if (recipe != null && (recipe.isApproved() || user.isAdmin())) {
                        recipeList.add(recipe);
                    }
                }
                recipeAdapter.setRecipeList(recipeList);
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

        // כפתור פתיחת מסך בקשות (Requests)
        btnAdminAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecipeRequestsActivity.class));
        });

        // כפתור פתיחת מסך הוספת מתכון
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
