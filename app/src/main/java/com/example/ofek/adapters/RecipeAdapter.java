package com.example.ofek.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onLongRecipeClick(Recipe recipe);
    }

    private final List<Recipe> recipeList;
    private final OnRecipeClickListener onRecipeClickListener;

    public RecipeAdapter(@Nullable final OnRecipeClickListener onRecipeClickListener) {
        this.recipeList = new ArrayList<>();
        this.onRecipeClickListener = onRecipeClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        if (recipe == null) return;

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvDescription.setText(recipe.getDescription());
        
        if (recipe.getCategory() != null && !recipe.getCategory().isEmpty()) {
            holder.chipCategory.setVisibility(View.VISIBLE);
            holder.chipCategory.setText(recipe.getCategory());
        } else {
            holder.chipCategory.setVisibility(View.GONE);
        }

        // Note: You might want to use a library like Glide or Picasso to load the imageUrl
        // if (recipe.getImageUrl() != null) { ... }

        holder.itemView.setOnClickListener(v -> {
            if (onRecipeClickListener != null) {
                onRecipeClickListener.onRecipeClick(recipe);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onRecipeClickListener != null) {
                onRecipeClickListener.onLongRecipeClick(recipe);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public void setRecipeList(List<Recipe> recipes) {
        recipeList.clear();
        recipeList.addAll(recipes);
        notifyDataSetChanged();
    }

    public void addRecipe(Recipe recipe) {
        recipeList.add(recipe);
        notifyItemInserted(recipeList.size() - 1);
    }

    public void updateRecipe(Recipe recipe) {
        for (int i = 0; i < recipeList.size(); i++) {
            if (recipeList.get(i).getId().equals(recipe.getId())) {
                recipeList.set(i, recipe);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeRecipe(Recipe recipe) {
        for (int i = 0; i < recipeList.size(); i++) {
            if (recipeList.get(i).getId().equals(recipe.getId())) {
                recipeList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        ImageView ivImage;
        Chip chipCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_recipe_title);
            tvDescription = itemView.findViewById(R.id.tv_recipe_description);
            ivImage = itemView.findViewById(R.id.iv_recipe_image);
            chipCategory = itemView.findViewById(R.id.chip_recipe_category);
        }
    }
}
