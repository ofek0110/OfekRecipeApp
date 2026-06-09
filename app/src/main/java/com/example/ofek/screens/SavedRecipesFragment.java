package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.FavoriteRecipe;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// פרגמנט המתכונים השמורים (חלק משלושת מסכי הליבה בקונטיינר הראשי).
// מציג למשתמש את כל המתכונים שהוא סימן להם לב (מועדפים) ואושרו על ידי האדמין.
public class SavedRecipesFragment extends Fragment {

    private RecyclerView rvSavedRecipes;
    private TextView tvEmptySavedState;
    private RecipeAdapter adapter;
    private List<Recipe> savedRecipesList = new ArrayList<>();
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ניפוח ה-XML של מסך המתכונים השמורים
        return inflater.inflate(R.layout.fragment_saved_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // שומר סף: שליפת המשתמש המחובר ועצירה אם אינו קיים
        currentUser = SharedPreferencesUtil.getUser(requireContext());
        if (currentUser == null) {
            return;
        }

        // אתחול רכיבי ה-UI וקישורם ל-XML
        rvSavedRecipes = view.findViewById(R.id.RvSavedRecipes);
        tvEmptySavedState = view.findViewById(R.id.TvEmptySavedState);

        // הגדרת הרשימה (RecyclerView) וחיבור האדפטר עם מאזין לחיצה לצפייה בפרטי המתכון
        rvSavedRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(currentUser.getId(), false, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(requireContext(), RecipeReviewActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) {}
            @Override
            public void onFavoriteClick(Recipe recipe, boolean isFavorite) {
                if (!isFavorite)
                    adapter.removeRecipe(recipe);
            }
        });
        rvSavedRecipes.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedRecipes();
    }

    // פנייה כפולה ל-Firebase:
    // 1. שליפת רשימת ה-FavoriteRecipe של המשתמש הנוכחי כדי לאסוף את ה-IDs של המתכונים שהוא אהב.
    // 2. שליפת רשימת המתכונים הכללית, וסינון שלה כך שישארו רק מתכונים מאושרים שה-ID שלהם נמצא ברשימת השמורים.
    private void loadSavedRecipes() {
        if (currentUser == null) return;
        String uid = currentUser.getId();

        DatabaseService.getInstance().getFavoriteRecipeByUser(uid, new DatabaseService.DatabaseCallback<List<FavoriteRecipe>>() {
            @Override
            public void onCompleted(List<FavoriteRecipe> favoriteRecipes) {
                // המרת רשימת המועדפים לסט של מזהי מתכונים (IDs) לצורך חיפוש מהיר
                Set<String> recipeIds = favoriteRecipes.stream().map(FavoriteRecipe::getRecipeId).collect(Collectors.toSet());

                DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
                    @Override
                    public void onCompleted(List<Recipe> recipes) {
                        // סינון קפדני: משאירים רק מתכונים מאושרים ורק כאלו שהמשתמש שמר במועדפים
                        recipes.removeIf(recipe -> !recipe.isApproved());
                        recipes.removeIf(recipe -> !recipeIds.contains(recipe.getId()));

                        savedRecipesList.clear();
                        savedRecipesList.addAll(recipes);

                        Collections.reverse(savedRecipesList);

                        // עדכון האדפטר ועדכון ה-UI למקרה שהרשימה ריקה
                        adapter.setRecipeList(savedRecipesList);
                        updateUI(savedRecipesList.isEmpty());
                    }

                    @Override
                    public void onFailed(Exception e) {}
                });
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // ניהול מצב מסך (Empty State): מציג הודעה מתאימה אם אין מתכונים שמורים, או מציג את הרשימה אם יש תוכן
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