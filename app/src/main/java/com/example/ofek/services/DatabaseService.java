package com.example.ofek.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ofek.models.FavoriteRecipe;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// מחלקת שירות שמנהלת את כל התקשורת מול Firebase Realtime Database
// המחלקה מעוצבת כ-Singleton (מופע יחיד בכל האפליקציה)
public class DatabaseService {

    private static final String TAG = "DatabaseService";

    // שמות התיקיות הראשיות (Nodes) בבסיס הנתונים שלנו
    private static final String USERS_PATH = "users",
            RECIPES_PATH = "recipes",
            FAVORITES_PATH = "favorites";

    // ממשק (Callback) גנרי להחזרת תשובה מ-Firebase (הצליח או נכשל)
    public interface DatabaseCallback<T> {
        public void onCompleted(@Nullable T object);
        public void onFailed(Exception e);
    }

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    // קונסטרקטור פרטי - מונע יצירת מופעים חיצוניים ומאתחל את ה-DB
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    // פונקציה לקבלת המופע היחיד של ה-Service
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // region private generic methods

    // כתיבת מידע כללי לנתיב מסוים ב-DB
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    // מחיקת מידע כללי מנתיב מסוים ב-DB
    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    // גישה לנתיב (Child) ספציפי בתוך ה-DB
    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    // שליפת אובייקט בודד מנתיב מסוים והמרתו למחלקה המבוקשת (clazz)
    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            if (!task.getResult().exists()) {
                callback.onCompleted(null);
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    // שליפת רשימת אובייקטים שלמה מתיקייה מסוימת
    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });

            callback.onCompleted(tList);
        });
    }

    // יצירת מפתח (ID) ייחודי ואוטומטי ב-Firebase עבור אובייקט חדש
    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    // הרצת טרנזקציה ב-DB (טוב לעדכון בטוח של ערכים בלי התנגשויות של כמה משתמשים במקביל)
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                if (currentValue == null) {
                    currentValue = function.apply(null);
                } else {
                    currentValue = function.apply(currentValue);
                }
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // endregion

    // region User Section

    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    // חיפוש משתמש לפי אימייל וסיסמה (בדיקת התחברות)
    public void getUserByEmailAndPassword(@NotNull final String email, @NotNull final String password, @NotNull final DatabaseCallback<User> callback) {
        getUserList(new DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email) && Objects.equals(user.getPassword(), password)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // בדיקה בהרשמה אם האימייל כבר תפוס במערכת
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        getUserList(new DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email)) {
                        callback.onCompleted(true);
                        return;
                    }
                }
                callback.onCompleted(false);
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    public void updateUser(@NotNull final String recipeId, UnaryOperator<User> function, @NotNull final DatabaseCallback<User> callback) {
        runTransaction(USERS_PATH + "/" + recipeId, User.class, function, callback);
    }

    // endregion User Section


    // region Recipes Section

    public String generateRecipeId() {
        return generateNewId(RECIPES_PATH);
    }

    public void createNewRecipe(@NotNull final Recipe recipe, @Nullable final DatabaseCallback<Void> callback) {
        writeData(RECIPES_PATH + "/" + recipe.getId(), recipe, callback);
    }

    public void getRecipe(@NotNull final String rid, @NotNull final DatabaseCallback<Recipe> callback) {
        getData(RECIPES_PATH + "/" + rid, Recipe.class, callback);
    }

    public void getRecipeList(@NotNull final DatabaseCallback<List<Recipe>> callback) {
        getDataList(RECIPES_PATH, Recipe.class, callback);
    }

    public void getAllRecipes(@NotNull final DatabaseCallback<List<Recipe>> callback) {
        getRecipeList(callback);
    }

    public void deleteRecipe(@NotNull final String rid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(RECIPES_PATH + "/" + rid, callback);
    }

    public void updateRecipes(@NotNull final String recipeId, UnaryOperator<Recipe> function, @NotNull final DatabaseCallback<Recipe> callback) {
        runTransaction(RECIPES_PATH + "/" + recipeId, Recipe.class, function, callback);
    }

    // endregion Recipes Section

    // region favorite Section

    public String generateFavoriteRecipeId() {
        return generateNewId(FAVORITES_PATH);
    }

    public void createNewFavoriteRecipe(@NotNull final FavoriteRecipe favoriteRecipe, @Nullable final DatabaseCallback<Void> callback) {
        writeData(FAVORITES_PATH + "/" + favoriteRecipe.getId(), favoriteRecipe, callback);
    }

    public void getFavoriteRecipe(@NotNull final String id, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
        getData(FAVORITES_PATH + "/" + id, FavoriteRecipe.class, callback);
    }

    public void getFavoriteRecipeList(@NotNull final DatabaseCallback<List<FavoriteRecipe>> callback) {
        getDataList(FAVORITES_PATH, FavoriteRecipe.class, callback);
    }

    public void deleteFavoriteRecipe(@NotNull final String id, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(FAVORITES_PATH + "/" + id, callback);
    }

    public void updateFavoriteRecipes(@NotNull final String id, UnaryOperator<FavoriteRecipe> function, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
        runTransaction(FAVORITES_PATH + "/" + id, FavoriteRecipe.class, function, callback);
    }

    // שליפת רשומת מועדף ספציפית לפי יוזר ומתכון (בודק אם המשתמש סימן לב על המתכון הזה)
    public void getFavoriteRecipeByUserAndRecipe(@NotNull final String uid, @NotNull final String rid, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
        Log.e("TAG", "getFavoriteRecipeByUserAndRecipe: " + uid + " " + rid);
        getFavoriteRecipeList(new DatabaseCallback<List<FavoriteRecipe>>() {
            @Override
            public void onCompleted(List<FavoriteRecipe> favoriteRecipes) {
                for (FavoriteRecipe favoriteRecipe: favoriteRecipes) {
                    if (Objects.equals(favoriteRecipe.getUserId(), uid) && Objects.equals(favoriteRecipe.getRecipeId(), rid)) {
                        callback.onCompleted(favoriteRecipe);
                        return;
                    }
                }
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // שליפת כל המתכונים המועדפים של משתמש ספציפי וסינון השאר
    public void getFavoriteRecipeByUser(@NotNull final String uid, @NotNull final DatabaseCallback<List<FavoriteRecipe>> callback) {
        getFavoriteRecipeList(new DatabaseCallback<List<FavoriteRecipe>>() {
            @Override
            public void onCompleted(List<FavoriteRecipe> favoriteRecipes) {
                favoriteRecipes.removeIf(new Predicate<FavoriteRecipe>() {
                    @Override
                    public boolean test(FavoriteRecipe favoriteRecipe) {
                        return !Objects.equals(favoriteRecipe.getUserId(), uid) ;
                    }
                });
                callback.onCompleted(favoriteRecipes);
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    // endregion favorite Section
}