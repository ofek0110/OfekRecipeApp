package com.example.ofek.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.models.FavoriteRecipe;
import com.example.ofek.models.Recipe;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.ImageUtil;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private final OnRecipeClickListener listener;
    private final String currentUserId;
    private final boolean showStatus;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onLongRecipeClick(Recipe recipe);
    }

    public RecipeAdapter(String currentUserId, boolean showStatus, OnRecipeClickListener listener) {
        this.currentUserId = currentUserId;
        this.showStatus = showStatus;
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
        holder.TvTitle.setText(recipe.getTitle());
        holder.TvPrepTime.setText(recipe.getPreparationTime());
        holder.TvDifficulty.setText(recipe.getDifficulty());
        holder.TvCategoryTag.setText(recipe.getCategory());

        if (showStatus) {
            holder.TvStatus.setVisibility(View.VISIBLE);
            if (recipe.isApproved()) {
                holder.TvStatus.setText("Approved");
                holder.TvStatus.setTextColor(Color.parseColor("#16A34A"));
            } else if (recipe.getAdminNotes() != null && !recipe.getAdminNotes().isEmpty()) {
                holder.TvStatus.setText("Needs Fixing");
                holder.TvStatus.setTextColor(Color.parseColor("#DC2626"));
            } else {
                holder.TvStatus.setText("Pending");
                holder.TvStatus.setTextColor(Color.parseColor("#D97706"));
            }
        } else {
            holder.TvStatus.setVisibility(View.GONE);
        }

        if (recipe.getImageBase64() != null)
            holder.IvImage.setImageBitmap(ImageUtil.convertFrom64base(recipe.getImageBase64()));

        checkIfFavorite(recipe.getId(), holder.IvFavoriteIcon);
        holder.FlFavoriteBtn.setOnClickListener(v -> toggleFavorite(recipe.getId(), holder.IvFavoriteIcon));
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    private void checkIfFavorite(String recipeId, ImageView heartIcon) {
        DatabaseService.getInstance().getFavoriteRecipeByUserAndRecipe(this.currentUserId, recipeId, new DatabaseService.DatabaseCallback<FavoriteRecipe>() {
            @Override
            public void onCompleted(@Nullable FavoriteRecipe favoriteRecipe) {
                if (favoriteRecipe == null)
                {
                    heartIcon.setAlpha(0.3f);
                }
                else {
                    heartIcon.setAlpha(1.0f);
                }
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }

    private void toggleFavorite(String recipeId, ImageView heartIcon) {
        DatabaseService.getInstance().getFavoriteRecipeByUserAndRecipe(this.currentUserId, recipeId, new DatabaseService.DatabaseCallback<FavoriteRecipe>() {
            @Override
            public void onCompleted(FavoriteRecipe favoriteRecipe) {
                if (favoriteRecipe != null) // exist
                {
                    DatabaseService.getInstance().deleteFavoriteRecipe(favoriteRecipe.getId(), new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(@Nullable Void object) {
                            heartIcon.setAlpha(0.3f);
                        }

                        @Override
                        public void onFailed(Exception e) {

                        }
                    });
                } else {
                    String id = DatabaseService.getInstance().generateFavoriteRecipeId();
                    FavoriteRecipe favoriteRecipe1 = new FavoriteRecipe(id, currentUserId, recipeId);
                    DatabaseService.getInstance().createNewFavoriteRecipe(favoriteRecipe1, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(@Nullable Void object) {
                            heartIcon.setAlpha(1.0f);
                        }

                        @Override
                        public void onFailed(Exception e) {

                        }
                    });
                }
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView TvTitle, TvPrepTime, TvDifficulty, TvCategoryTag, TvStatus;
        ImageView IvImage, IvFavoriteIcon;
        FrameLayout FlFavoriteBtn;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            TvTitle = itemView.findViewById(R.id.TvRecipeTitle);
            TvPrepTime = itemView.findViewById(R.id.TvPrepTime);
            TvDifficulty = itemView.findViewById(R.id.TvDifficulty);
            TvCategoryTag = itemView.findViewById(R.id.TvCategoryTag);
            TvStatus = itemView.findViewById(R.id.TvStatus);
            IvImage = itemView.findViewById(R.id.IvRecipeImage);
            FlFavoriteBtn = itemView.findViewById(R.id.FlFavoriteBtn);
            IvFavoriteIcon = itemView.findViewById(R.id.IvFavoriteIcon);

            itemView.setOnClickListener(v -> listener.onRecipeClick(recipeList.get(getAdapterPosition())));
            itemView.setOnLongClickListener(v -> {
                listener.onLongRecipeClick(recipeList.get(getAdapterPosition()));
                return true;
            });
        }
    }
}