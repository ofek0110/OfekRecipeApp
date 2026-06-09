package com.example.ofek.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// מסך הבית של האפליקציה (בנוי כפרגמנט שמתלבש בתוך הקונטיינר הראשי).
// מציג את כל המתכונים המאושרים, מאפשר חיפוש, סינון לפי קטגוריות, ומכיל את תפריט הניהול של האדמין.
public class HomeFragment extends Fragment {

    // הגדרת רכיבי ה-UI ומשתנים גלובליים לניהול רשימות המתכונים והסינונים
    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();
    private List<Recipe> filteredList = new ArrayList<>();
    private EditText etSearch;
    private LinearLayout headerButtons;
    private TextView btnUsers, btnRequests, btnManageRecipes, tvRequestsBadge;
    private FloatingActionButton fabAddRecipe;
    private ImageView ivMyRecipes;

    private String currentCategoryFilter = "";
    private MaterialCardView cardBreakfast, cardLunch, cardVegan, cardDesserts, cardDinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ניפוח ה-XML של מסך הבית
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // בדיקת אבטחה: אם המשתמש לא מחובר, נעיף אותו חזרה למסך ההתחברות
        User currentUser = SharedPreferencesUtil.getUser(requireContext());
        if (currentUser == null) {
            startActivity(new Intent(requireActivity(), LogIn.class));
            requireActivity().finish();
            return;
        }
        String currentUserId = currentUser.getId();

        // קישור משתני ה-UI לרכיבים בקובץ ה-XML
        rvRecipes = view.findViewById(R.id.RvRecipes);
        etSearch = view.findViewById(R.id.EtSearch);
        headerButtons = view.findViewById(R.id.HeaderButtons);
        btnUsers = view.findViewById(R.id.BtnUsers);
        btnRequests = view.findViewById(R.id.BtnRequests);
        btnManageRecipes = view.findViewById(R.id.BtnManageRecipes);
        tvRequestsBadge = view.findViewById(R.id.TvRequestsBadge);
        fabAddRecipe = view.findViewById(R.id.FabAddRecipe);
        ivMyRecipes = view.findViewById(R.id.IvMyRecipes);

        // בדיקת הרשאות אדמין: אם המשתמש הוא אדמין, נציג לו את כפתורי הניהול העליונים, אחרת נסטיר אותם
        if (!currentUser.isAdmin()) {
            headerButtons.setVisibility(View.GONE);
        }

        // אתחול ה-RecyclerView והגדרת האדפטר של המתכונים
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(currentUserId, false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                // לחיצה על מתכון פותחת את מסך הפירוט והבדיקה שלו
                Intent intent = new Intent(requireContext(), RecipeReviewActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {}
            @Override
            public void onFavoriteClick(Recipe recipe, boolean isFavorite) {}
        });
        rvRecipes.setAdapter(adapter);

        // הגדרת מאזיני הלחיצה עבור כרטיסיות הקטגוריות
        setupCategoryFilters(view);

        // הגדרת הניווט ללחיצות על שאר כפתורי המסך (המתכונים שלי, הוספת מתכון, ומסכי האדמין)
        ivMyRecipes.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyRecipesActivity.class)));
        fabAddRecipe.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddRecipeActivity.class)));
        btnUsers.setOnClickListener(v -> startActivity(new Intent(requireContext(), UsersList.class)));
        btnRequests.setOnClickListener(v -> startActivity(new Intent(requireContext(), RecipeRequestsActivity.class)));
        btnManageRecipes.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminRecipeManagerActivity.class)));

        // האזנה בזמן אמת להקלדה בתיבת החיפוש ועדכון הרשימה בהתאם לטקסט שהוזן
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
    }

    @Override
    public void onResume() {
        super.onResume();
        // בכל פעם שחוזרים למסך, נמשוך מחדש את המתכונים המעודכנים מ-Database
        loadRecipes();
    }

    // אתחול כרטיסיות הקטגוריות והגדרת מאזיני לחיצה שיפעילו את הסינון המתאים
    private void setupCategoryFilters(View view) {
        view.findViewById(R.id.CatBreakfast).setOnClickListener(v -> filterByCategory("Breakfast"));
        view.findViewById(R.id.CatLunch).setOnClickListener(v -> filterByCategory("Lunch"));
        view.findViewById(R.id.CatVegan).setOnClickListener(v -> filterByCategory("Vegan"));
        view.findViewById(R.id.CatDesserts).setOnClickListener(v -> filterByCategory("Desserts"));
        view.findViewById(R.id.CatDinner).setOnClickListener(v -> filterByCategory("Dinner"));
        view.findViewById(R.id.TvSeeAllCategories).setOnClickListener(v -> filterByCategory(""));

        cardBreakfast = view.findViewById(R.id.CardBreakfast);
        cardLunch = view.findViewById(R.id.CardLunch);
        cardVegan = view.findViewById(R.id.CardVegan);
        cardDesserts = view.findViewById(R.id.CardDesserts);
        cardDinner = view.findViewById(R.id.CardDinner);
    }

    // קביעת הקטגוריה הנוכחית שנבחרה והפעלת פונקציות הסינון וצביעת הכרטיסיות
    private void filterByCategory(String category) {
        currentCategoryFilter = category;
        updateCategoryColors(category);
        filterRecipes(etSearch.getText().toString());
    }

    // לוגיקה ויזואלית: מאפסת את צבעי כל הקטגוריות לצבע בהיר ומדגישה בצבע כהה יותר רק את הקטגוריה שנבחרה
    private void updateCategoryColors(String selectedCategory) {
        cardBreakfast.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
        cardLunch.setCardBackgroundColor(Color.parseColor("#FFEDD5"));
        cardVegan.setCardBackgroundColor(Color.parseColor("#DCFCE7"));
        cardDesserts.setCardBackgroundColor(Color.parseColor("#FCE7F3"));
        cardDinner.setCardBackgroundColor(Color.parseColor("#FEF9C3"));

        switch (selectedCategory) {
            case "Breakfast": cardBreakfast.setCardBackgroundColor(Color.parseColor("#80CBC4")); break;
            case "Lunch": cardLunch.setCardBackgroundColor(Color.parseColor("#FDBA74")); break;
            case "Vegan": cardVegan.setCardBackgroundColor(Color.parseColor("#86EFAC")); break;
            case "Desserts": cardDesserts.setCardBackgroundColor(Color.parseColor("#F9A8D4")); break;
            case "Dinner": cardDinner.setCardBackgroundColor(Color.parseColor("#FDE047")); break;
        }
    }

    /// מנגנון הסינון המשולב: רץ על הרשימה המקורית ומסנן מתכונים שעומדים גם בתנאי החיפוש החופשי וגם בקטגוריה שנבחרה
    private void filterRecipes(String text) {
        filteredList.clear();
        for (Recipe recipe : recipeList) {
            boolean matchesSearch = recipe.getTitle().toLowerCase().contains(text.toLowerCase()); // בדיקת התנאי של הכותרת
            // בדיקת התנאי של הקטגוריה
            boolean matchesCategory = currentCategoryFilter.isEmpty() ||
                    (recipe.getCategory() != null && recipe.getCategory().equalsIgnoreCase(currentCategoryFilter));
            /// בדיקת שני התנאיפ
            if (matchesSearch && matchesCategory) {
                filteredList.add(recipe);
            }
        }

        adapter.setRecipeList(filteredList);
    }

    /// שליפת כל המתכונים מ-Database, חישוב כמות הבקשות הממתינות (עבור ה-Badge של האדמין), וסינון כך שיוצגו רק מתכונים מאושרים
    private void loadRecipes() {
        DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
            @Override
            public void onCompleted(@Nullable List<Recipe> recipes) {
                if (recipes == null) return;
                if (!isAdded()) return;

                int pendingCount = (int) recipes.stream().filter(Recipe::isPending).count();

                recipes.removeIf(recipe -> !recipe.isApproved());

                recipeList.clear();
                recipeList.addAll(recipes);
                Collections.reverse(recipeList);

                filterRecipes(etSearch.getText().toString());

                if (pendingCount > 0) {
                    tvRequestsBadge.setVisibility(View.VISIBLE);
                    tvRequestsBadge.setText(pendingCount > 99 ? "99+" : String.valueOf(pendingCount));
                } else {
                    tvRequestsBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }
}