package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
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
    private FloatingActionButton fabAddRecipe;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול רכיבים מה-XML
        rvRecipes = findViewById(R.id.rvRecipes);
        etSearch = findViewById(R.id.etSearch);
        btnUsers = findViewById(R.id.btnUsers);         // הכפתור הירוק
        btnRequests = findViewById(R.id.btnRequests);   // הכפתור הכתום
        fabAddRecipe = findViewById(R.id.fabAddRecipe);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // הגדרת RecyclerView
        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        recipeList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(MainActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {
                // אופציונלי: כאן ניתן להוסיף פעולה בלחיצה ארוכה
            }
        });

        rvRecipes.setAdapter(adapter);

        // הגדרת פעולות לכפתורים
        fabAddRecipe.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddRecipeActivity.class));
        });

        btnUsers.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UsersList.class));
        });

        btnRequests.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecipeRequestsActivity.class));
        });

        // הגדרת חיפוש
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

        // הגדרת הבר התחתון
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // אנחנו כבר בבית, אולי לגלול למעלה?
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, UserProfile.class));
                return true;
            } else if (itemId == R.id.nav_explore) {
                // אופציונלי: מסך נוסף או רענון
                return true;
            }
            return false;
        });

        // טעינת הנתונים
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
                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    // מציגים רק מתכונים שאושרו (isApproved = true)
                    if (recipe != null && recipe.isApproved()) {
                        recipeList.add(recipe);
                    }
                }
                // היפוך הרשימה כדי לראות את החדשים ביותר ראשונים
                Collections.reverse(recipeList);

                filteredList.clear();
                filteredList.addAll(recipeList);
                adapter.setRecipeList(filteredList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}