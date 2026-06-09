package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
import java.util.stream.Collectors;

// מסך מנהל מערכת (אדמין) לניהול, סינון וצפייה בכל המתכונים שקיימים באפליקציה
public class AdminRecipeManagerActivity extends AppCompatActivity {

    private RecyclerView rvAdminRecipes;
    private RecipeAdapter adapter;
    private List<Recipe> allRecipes = new ArrayList<>();
    private List<Recipe> filteredRecipes = new ArrayList<>();
    private SearchView searchView;
    private Spinner statusSpinner;
    private DatabaseService databaseService;
    private String currentQuery = "";
    private String currentStatusFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_recipe_manager);

        // אבטחה ושומרי סף: בדיקה שהמשתמש הנוכחי מחובר והוא אכן אדמין, אחרת נחסום ונזרוק אותו מהמסך
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseService = DatabaseService.getInstance();
        rvAdminRecipes = findViewById(R.id.rvAdminRecipes);
        searchView = findViewById(R.id.searchView);
        statusSpinner = findViewById(R.id.statusSpinner);

        // הגדרת ה-RecyclerView וחיבור האדפטר. התיקון בוצע בתוך ה-onRecipeClick לשליחת ה-ID המתאים
        rvAdminRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(currentUser.getId(), true, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                // התיקון: שולחים את ה-ID של המתכון תחת המפתח "recipe_id" כדי להתאים לציפיות של RecipeReviewActivity
                Intent intent = new Intent(AdminRecipeManagerActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {}
        });
        rvAdminRecipes.setAdapter(adapter);

        // הפעלת מאזיני שינוי קלט עבור תיבת החיפוש ותפריט הסינון (Spinner)
        setupFilters();


    }

    @Override
    protected void onResume() {
        super.onResume();

        // משיכה ראשונית של כל המתכונים מה-DB
        loadAllRecipes();
    }

    // הגדרת מאזינים לחיפוש טקסט חופשי ולבחירת סטטוס מה-Spinner (בכל שינוי נקרא לפונקציית הסינון)
    private void setupFilters() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.toLowerCase();
                applyFilters();
                return true;
            }
        });

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusFilter = parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // שליפת רשימת כל המתכונים הגלובלית מ-Firebase
    private void loadAllRecipes() {
        databaseService.getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
            @Override
            public void onCompleted(List<Recipe> recipes) {
                if (recipes != null) {
                    allRecipes = recipes;
                    applyFilters();
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AdminRecipeManagerActivity.this, "Error loading recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // לוגיקת הסינון: משתמשת ב-Streams כדי לסנן את רשימת כל המתכונים על פי הטקסט שבחיפוש והסטטוס שנבחר (Pending/Approved/Rejected)
    private void applyFilters() {
        filteredRecipes = allRecipes.stream().filter(recipe -> {
            boolean matchesQuery = recipe.getTitle().toLowerCase().contains(currentQuery) ||
                    (recipe.getDescription() != null && recipe.getDescription().toLowerCase().contains(currentQuery));

            boolean matchesStatus = true;
            if (currentStatusFilter.equals("Pending")) {
                matchesStatus = recipe.isPending();
            } else if (currentStatusFilter.equals("Approved")) {
                matchesStatus = recipe.isApproved();
            } else if (currentStatusFilter.equals("Rejected")) {
                matchesStatus = !recipe.isApproved() && !recipe.isPending(); // תנאי מותאם לסטטוס דחוי
            }

            return matchesQuery && matchesStatus;
        }).collect(Collectors.toList());

        adapter.setRecipeList(filteredRecipes);
    }
}