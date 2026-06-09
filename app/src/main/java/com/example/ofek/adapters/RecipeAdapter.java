package com.example.ofek.adapters;

import android.graphics.Color;
import android.util.Log;
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
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.ImageUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// אדפטר לניהול והצגת רשימת המתכונים ב-RecyclerView
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private final OnRecipeClickListener listener;
    private final String currentUserId;
    private final boolean showStatus; // דגל: האם להציג סטטוס מתכון (עבור אדמין או יוצר המתכון)
    private final Map<String, User> usersCache = new HashMap<>(); // קאש לשמות משתמשים כדי לא להעמיס קריאות ל-Firebase

    // ממשק להאזנה ללחיצות (רגילה וארוכה) על מתכון ברשימה
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onLongRecipeClick(Recipe recipe);
        void onFavoriteClick(Recipe recipe, boolean isFavorite);
    }

    public RecipeAdapter(@NotNull String currentUserId, boolean showStatus, OnRecipeClickListener listener) {
        this.currentUserId = currentUserId;
        this.showStatus = showStatus;
        this.listener = listener;
        this.recipeList = new ArrayList<>();
    }

    // עדכון הרשימה מחדש ורענון ה-UI
    public void setRecipeList(List<Recipe> recipeList) {
        this.recipeList = recipeList;
        if (this.recipeList == null) this.recipeList = new ArrayList<>();
        notifyDataSetChanged();
    }
    public void removeRecipe(Recipe recipe) {
        int index = recipeList.indexOf(recipe);
        if (index == -1) return;
        recipeList.remove(index);
        notifyItemRemoved(index);
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ניפוח ה-XML של שורת מתכון בודדת
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        // טעינת נתונים בסיסיים של המתכון לתוך ה-Views
        holder.TvTitle.setText(recipe.getTitle());
        holder.TvPrepTime.setText(recipe.getPreparationTime());
        holder.TvDifficulty.setText(recipe.getDifficulty());
        holder.TvCategoryTag.setText(recipe.getCategory());

        // הצגת דירוג או סימון כחדש
        if (recipe.getTotalRaters() > 0) {
            holder.TvItemRating.setText(String.format(java.util.Locale.US, "%.1f", recipe.getRating()));
        } else {
            holder.TvItemRating.setText("New");
        }

        // אם צריך להציג סטטוס (אישור/המתנה/תיקון) ושם כותב - בעיקר למסכי ניהול ואדמין
        if (showStatus) {
            holder.TvStatus.setVisibility(View.VISIBLE);
            if (recipe.isApproved()) {
                holder.TvStatus.setText("Approved");
                holder.TvStatus.setTextColor(Color.parseColor("#16A34A")); // ירוק
            } else if (recipe.getAdminNotes() != null && !recipe.getAdminNotes().isEmpty()) {
                holder.TvStatus.setText("Needs Fixing");
                holder.TvStatus.setTextColor(Color.parseColor("#DC2626")); // אדום
            } else {
                holder.TvStatus.setText("Pending");
                holder.TvStatus.setTextColor(Color.parseColor("#D97706")); // כתום
            }

            holder.TvAuthorName.setVisibility(View.VISIBLE);
            fetchAuthorName(recipe.getUserId(), holder.TvAuthorName); // משיכת שם היוצר מה-DB
        } else {
            holder.TvStatus.setVisibility(View.GONE);
            holder.TvAuthorName.setVisibility(View.GONE);
        }

        // פענוח והצגת תמונת המתכון מ-Base64, או תמונת ברירת מחדל
        if (recipe.getImageBase64() != null && !recipe.getImageBase64().isEmpty())
            holder.IvImage.setImageBitmap(ImageUtil.convertFrom64base(recipe.getImageBase64()));
        else
            holder.IvImage.setImageResource(R.drawable.ic_launcher_background);

        // בדיקה אם המתכון במועדפים ועדכון הלב, פלוס האזנה ללחיצה על הלב
        checkIfFavorite(recipe.getId(), holder.IvFavoriteIcon);
        holder.FlFavoriteBtn.setOnClickListener(v -> toggleFavorite(recipe, holder.IvFavoriteIcon));
    }

    // משיכת שם המשתמש שיצר את המתכון (משתמש בקאש המקומי למניעת כפילויות)
    private void fetchAuthorName(String userId, TextView tvAuthor) {
        if (usersCache.containsKey(userId)) {
            User user = usersCache.get(userId);
            String fullName = user.getFirstname() + " " + user.getLastname();
            tvAuthor.setText("by " + fullName);
            return;
        }

        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(@Nullable User user) {
                if (user != null) {
                    usersCache.put(userId, user);
                    String fullName = user.getFirstname() + " " + user.getLastname();
                    tvAuthor.setText("by " + fullName);
                } else {
                    tvAuthor.setText("by Unknown");
                }
            }

            @Override
            public void onFailed(Exception e) {
                tvAuthor.setText("by Unknown");
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    // בדיקה מול Firebase אם המתכון מסומן כאהוב ע"י המשתמש הנוכחי ושינוי שקיפות הלב בהתאם
    private void checkIfFavorite(String recipeId, ImageView heartIcon) {
        DatabaseService.getInstance().getFavoriteRecipeByUserAndRecipe(this.currentUserId, recipeId, new DatabaseService.DatabaseCallback<FavoriteRecipe>() {
            @Override
            public void onCompleted(@Nullable FavoriteRecipe favoriteRecipe) {
                if (favoriteRecipe == null) {
                    heartIcon.setAlpha(0.3f); // לא במועדפים - לב חצי שקוף
                } else {
                    heartIcon.setAlpha(1.0f); // במועדפים - לב מלא
                }
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // הוספה או הסרה של המתכון מהמועדפים בלחיצה (מחיקה או יצירה ב-Firebase)
    private void toggleFavorite(Recipe recipe, ImageView heartIcon) {
        String recipeId = recipe.getId();
        DatabaseService.getInstance().getFavoriteRecipeByUserAndRecipe(this.currentUserId, recipeId, new DatabaseService.DatabaseCallback<FavoriteRecipe>() {
            @Override
            public void onCompleted(FavoriteRecipe favoriteRecipe) {
                if (favoriteRecipe != null) {
                    // כבר קיים? מוחקים מהמועדפים
                    DatabaseService.getInstance().deleteFavoriteRecipe(favoriteRecipe.getId(), new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(@Nullable Void object) {
                            heartIcon.setAlpha(0.3f);
                            RecipeAdapter.this.listener.onFavoriteClick(recipe, false);
                        }
                        @Override
                        public void onFailed(Exception e) {}
                    });
                } else {
                    // לא קיים? מייצרים רשומה חדשה במועדפים
                    String id = DatabaseService.getInstance().generateFavoriteRecipeId();
                    FavoriteRecipe fav = new FavoriteRecipe(id, recipeId, currentUserId);
                    DatabaseService.getInstance().createNewFavoriteRecipe(fav, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(@Nullable Void object) {
                            heartIcon.setAlpha(1.0f);
                            RecipeAdapter.this.listener.onFavoriteClick(recipe, true);
                        }
                        @Override
                        public void onFailed(Exception e) {}
                    });
                }
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // מחזיק ה-Views של שורת המתכון + הגדרת מאזיני לחיצה על כל השורה
    class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView TvTitle, TvPrepTime, TvDifficulty, TvCategoryTag, TvStatus, TvItemRating, TvAuthorName;
        ImageView IvImage, IvFavoriteIcon;
        FrameLayout FlFavoriteBtn;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            TvTitle = itemView.findViewById(R.id.TvRecipeTitle);
            TvAuthorName = itemView.findViewById(R.id.TvAuthorName);
            TvPrepTime = itemView.findViewById(R.id.TvPrepTime);
            TvDifficulty = itemView.findViewById(R.id.TvDifficulty);
            TvCategoryTag = itemView.findViewById(R.id.TvCategoryTag);
            TvStatus = itemView.findViewById(R.id.TvStatus);
            TvItemRating = itemView.findViewById(R.id.TvItemRating);

            IvImage = itemView.findViewById(R.id.IvRecipeImage);
            FlFavoriteBtn = itemView.findViewById(R.id.FlFavoriteBtn);
            IvFavoriteIcon = itemView.findViewById(R.id.IvFavoriteIcon);

            // לחיצה רגילה לפתיחת המתכון
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onRecipeClick(recipeList.get(pos));
                }
            });
            // לחיצה ארוכה (טוב לאפשרויות מחיקה/עריכה)
            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onLongRecipeClick(recipeList.get(pos));
                }
                return true;
            });
        }
    }
}