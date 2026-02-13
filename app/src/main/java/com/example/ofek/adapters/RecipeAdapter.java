package com.example.ofek.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private final OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onLongRecipeClick(Recipe recipe);
    }

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRecipeList(List<Recipe> recipeList) {
        this.recipeList = recipeList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        holder.tvTitle.setText(recipe.getTitle());
        holder.tvPrepTime.setText(recipe.getPreparationTime());
        holder.tvDifficulty.setText(recipe.getDifficulty());
        holder.tvCategoryTag.setText(recipe.getCategory());
        // כאן ניתן להשתמש ב-Glide לטעינת התמונה מה-URL
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrepTime, tvDifficulty, tvCategoryTag;
        ImageView ivImage;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRecipeTitle);
            tvPrepTime = itemView.findViewById(R.id.tvPrepTime);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvCategoryTag = itemView.findViewById(R.id.tvCategoryTag);
            ivImage = itemView.findViewById(R.id.ivRecipeImage);

            itemView.setOnClickListener(v -> listener.onRecipeClick(recipeList.get(getAdapterPosition())));
            itemView.setOnLongClickListener(v -> {
                listener.onLongRecipeClick(recipeList.get(getAdapterPosition()));
                return true;
            });
        }
    }
}