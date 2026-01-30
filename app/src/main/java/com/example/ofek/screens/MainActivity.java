package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private FloatingActionButton fabAddRecipe;
    private BottomNavigationView bottomNavigationView;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // הגדרת Padding למסך מלא למניעת הסתרה ע"י ה-Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentUser = SharedPreferencesUtil.getUser(this);

        initializeViews();
        setupRecipeList();
        setupClickListeners();
        loadRecipes();
    }

    private void initializeViews() {
        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes);
        fabAddRecipe = findViewById(R.id.fabAddRecipe);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupRecipeList() {
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                // מעבר למסך הצפייה במתכון עם העברת האובייקט
                Intent intent = new Intent(MainActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {
                // אופציונלי: כאן אפשר להוסיף לוגיקה למחיקה ע"י אדמין
            }
        });

        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecipes.setAdapter(recipeAdapter);
    }

    private void setupClickListeners() {
        // לחיצה על כפתור הפלוס - הוספת מתכון
        fabAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddRecipeActivity.class));
        });

        // לחיצה על התפריט התחתון
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // גלילה לראש הרשימה
                    recyclerViewRecipes.smoothScrollToPosition(0);
                    return true;
                }
                else if (itemId == R.id.nav_profile) {
                    // מעבר למסך הפרופיל
                    startActivity(new Intent(MainActivity.this, UserProfile.class));
                    return true;
                }
                else if (itemId == R.id.nav_explore || itemId == R.id.nav_saved) {
                    Toast.makeText(MainActivity.this, "Coming Soon!", Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            }
        });
    }

    private void loadRecipes() {
        DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    // הצגת מתכונים: רק מאושרים, אלא אם המשתמש הוא מנהל
                    if (recipe != null) {
                        if (recipe.isApproved() || (currentUser != null && currentUser.isAdmin())) {
                            recipeList.add(recipe);
                        }
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
}