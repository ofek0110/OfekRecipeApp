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

public class SavedRecipesFragment extends Fragment {

    private RecyclerView rvSavedRecipes;
    private TextView tvEmptySavedState;
    private RecipeAdapter adapter;
    private List<Recipe> savedRecipesList = new ArrayList<>();
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUser = SharedPreferencesUtil.getUser(requireContext());
        if (currentUser == null) {
            return;
        }

        rvSavedRecipes = view.findViewById(R.id.RvSavedRecipes);
        tvEmptySavedState = view.findViewById(R.id.TvEmptySavedState);

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
        });

        rvSavedRecipes.setAdapter(adapter);

        loadSavedRecipes();
    }

    private void loadSavedRecipes() {
        if (currentUser == null) return;
        String uid = currentUser.getId();
        DatabaseService.getInstance().getFavoriteRecipeByUser(uid, new DatabaseService.DatabaseCallback<List<FavoriteRecipe>>() {
            @Override
            public void onCompleted(List<FavoriteRecipe> favoriteRecipes) {
                Set<String> recipeIds = favoriteRecipes.stream().map(FavoriteRecipe::getRecipeId).collect(Collectors.toSet());
                DatabaseService.getInstance().getRecipeList(new DatabaseService.DatabaseCallback<List<Recipe>>() {
                    @Override
                    public void onCompleted(List<Recipe> recipes) {
                        recipes.removeIf(recipe -> !recipe.isApproved());
                        recipes.removeIf(recipe -> !recipeIds.contains(recipe.getId()));
                        savedRecipesList.clear();
                        savedRecipesList.addAll(recipes);
                        adapter.setRecipeList(savedRecipesList);
                        updateUI(savedRecipesList.isEmpty());

                    }

                    @Override
                    public void onFailed(Exception e) {

                    }
                });
            }

            @Override
            public void onFailed(Exception e) {

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