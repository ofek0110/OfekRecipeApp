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
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedRecipesFragment extends Fragment {

    private RecyclerView rvSavedRecipes;
    private TextView tvEmptySavedState;
    private RecipeAdapter adapter;
    private List<Recipe> savedRecipesList = new ArrayList<>();
    private DatabaseReference favoritesRef;
    private DatabaseReference recipesRef;
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
                intent.putExtra("recipe", recipe);
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
        favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(uid);
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot favoritesSnapshot) {
                if (!isAdded()) return;
                savedRecipesList.clear();

                if (!favoritesSnapshot.exists() || !favoritesSnapshot.hasChildren()) {
                    updateUI(true);
                    return;
                }

                List<String> favoriteIds = new ArrayList<>();
                for (DataSnapshot child : favoritesSnapshot.getChildren()) {
                    if (child.getKey() != null) {
                        favoriteIds.add(child.getKey());
                    }
                }

                final int totalFavorites = favoriteIds.size();
                final int[] loadedCount = {0};

                for (String recipeId : favoriteIds) {
                    recipesRef.child(recipeId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot recipeSnapshot) {
                            if (!isAdded()) return;
                            Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                            if (recipe != null) {
                                savedRecipesList.add(recipe);
                            }
                            loadedCount[0]++;
                            if (loadedCount[0] == totalFavorites) {
                                Collections.reverse(savedRecipesList);
                                adapter.setRecipeList(savedRecipesList);
                                updateUI(savedRecipesList.isEmpty());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedCount[0]++;
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) Toast.makeText(requireContext(), "Error loading saved recipes", Toast.LENGTH_SHORT).show();
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