package com.example.ofek.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;

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

        // עדכון הכותרת
        holder.tvTitle.setText(recipe.getTitle());

        // עדכון קטגוריות
        // בעיצוב החדש יש לנו LinearLayout של תגיות.
        // כאן אנחנו מנקים תגיות ישנות ומוסיפים את הקטגוריה הנוכחית
        holder.llTags.removeAllViews();

        if (recipe.getCategory() != null && !recipe.getCategory().isEmpty()) {
            addTagToLayout(holder.llTags, recipe.getCategory(), R.color.tag_pink_bg, R.color.tag_pink_text);
        }

        // אופציונלי: הוספת רמת קושי כתגית שנייה
        if (recipe.getDifficulty() != null && !recipe.getDifficulty().isEmpty()) {
            addTagToLayout(holder.llTags, recipe.getDifficulty(), R.color.tag_green_bg, R.color.tag_green_text);
        }

        // עדכון זמן הכנה (אם הוספת ID לטקסט הזמן ב-XML, כרגע זה מוסתר כי לא היה ID)
        // holder.tvTime.setText(recipe.getPreparationTime());

        // טיפול בלחיצות
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

    // פונקציה ליצירת תגית מעוצבת דינמית
    private void addTagToLayout(LinearLayout layout, String text, int bgColorRes, int textColorRes) {
        TextView tag = new TextView(layout.getContext());
        tag.setText(text);
        tag.setTextSize(10);
        tag.setTypeface(null, android.graphics.Typeface.BOLD);
        tag.setTextColor(layout.getContext().getColor(textColorRes));
        tag.setBackgroundResource(R.drawable.bg_tag_rounded);
        tag.setBackgroundTintList(layout.getContext().getColorStateList(bgColorRes));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 0); // מרווח בין תגיות
        tag.setLayoutParams(params);

        // ריפוד פנימי (Padding)
        int paddingHorz = dpToPx(layout, 12);
        int paddingVert = dpToPx(layout, 4);
        tag.setPadding(paddingHorz, paddingVert, paddingHorz, paddingVert);

        layout.addView(tag);
    }

    private int dpToPx(View view, int dp) {
        return (int) (dp * view.getResources().getDisplayMetrics().density);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView ivImage;
        LinearLayout llTags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // התאמה ל-IDs החדשים ב-item_recipe.xml
            tvTitle = itemView.findViewById(R.id.tvRecipeTitle);
            ivImage = itemView.findViewById(R.id.ivRecipeImage);
            llTags = itemView.findViewById(R.id.llTags);
        }
    }
}