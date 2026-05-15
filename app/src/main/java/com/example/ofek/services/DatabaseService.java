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


/// a service to interact with the Firebase Realtime Database.
/// this class is a singleton, use getInstance() to get an instance of this class
/// @see #getInstance()
/// @see FirebaseDatabase
public class DatabaseService {

    /// tag for logging
    /// @see Log
    private static final String TAG = "DatabaseService";

    /// paths for different data types in the database
    /// @see DatabaseService#readData(String)
    private static final String USERS_PATH = "users",
            RECIPES_PATH = "recipes",
            FAVORITES_PATH = "favorites";

    /// callback interface for database operations
    /// @param <T> the type of the object to return
    /// @see DatabaseCallback#onCompleted(Object)
    /// @see DatabaseCallback#onFailed(Exception)
    public interface DatabaseCallback<T> {
        /// called when the operation is completed successfully
        public void onCompleted(@Nullable T object);

        /// called when the operation fails with an exception
        public void onFailed(Exception e);
    }

    /// the instance of this class
    /// @see #getInstance()
    private static DatabaseService instance;

    /// the reference to the database
    /// @see DatabaseReference
    /// @see FirebaseDatabase#getReference()
    private final DatabaseReference databaseReference;

    /// use getInstance() to get an instance of this class
    /// @see DatabaseService#getInstance()
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    /// get an instance of this class
    /// @return an instance of this class
    /// @see DatabaseService
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }


    // region private generic methods
    // to write and read data from the database

    /// write data to the database at a specific path
    /// @param path the path to write the data to
    /// @param data the data to write (can be any object, but must be serializable, i.e. must have a default constructor and all fields must have getters and setters)
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
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

    /// remove data from the database at a specific path
    /// @param path the path to remove the data from
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
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

    /// read data from the database at a specific path
    /// @param path the path to read the data from
    /// @return a DatabaseReference object to read the data from
    /// @see DatabaseReference

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }


    /// get data from the database at a specific path
    /// @param path the path to get the data from
    /// @param clazz the class of the object to return
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    /// @see Class
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

    /// get a list of data from the database at a specific path
    /// @param path the path to get the data from
    /// @param clazz the class of the objects to return
    /// @param callback the callback to call when the operation is completed
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

    /// generate a new id for a new object in the database
    /// @param path the path to generate the id for
    /// @return a new id for the object
    /// @see String
    /// @see DatabaseReference#push()

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }


    /// run a transaction on the data at a specific path </br>
    /// good for incrementing a value or modifying an object in the database
    /// @param path the path to run the transaction on
    /// @param clazz the class of the object to return
    /// @param function the function to apply to the current value of the data
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseReference#runTransaction(Transaction.Handler)
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

    // endregion of private methods for reading and writing data

    // public methods to interact with the database

    // region User Section

    /// generate a new id for a new user in the database
    /// @return a new id for the user
    /// @see #generateNewId(String)
    /// @see User
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    /// create a new user in the database
    /// @param user the user object to create
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive void
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    /// get a user from the database
    /// @param uid the id of the user to get
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive the user object
    ///             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    /// get all the users from the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive a list of user objects
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see User
    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    /// delete a user from the database
    /// @param uid the user id to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    /// get a user by email and password
    /// @param email the email of the user
    /// @param password the password of the user
    /// @param callback the callback to call when the operation is completed
    ///            the callback will receive the user object
    ///          if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
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
            public void onFailed(Exception e) {

            }
        });
    }

    /// check if an email already exists in the database
    /// @param email the email to check
    /// @param callback the callback to call when the operation is completed
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
            public void onFailed(Exception e) {

            }
        });
    }

    public void updateUser(@NotNull final String recipeId, UnaryOperator<User> function, @NotNull final DatabaseCallback<User> callback) {
        runTransaction(USERS_PATH + "/" + recipeId, User.class, function, callback);
    }

    // endregion User Section


    // region Recipes Section

    /// generate a new id for a new recipe in the database
    /// @return a new id for the user
    /// @see #generateNewId(String)
    /// @see User
    public String generateRecipeId() {
        return generateNewId(RECIPES_PATH);
    }

    /// create a new recipe in the database
    /// @param recipe the recipe object to create
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive void
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void createNewRecipe(@NotNull final Recipe recipe, @Nullable final DatabaseCallback<Void> callback) {
        writeData(RECIPES_PATH + "/" + recipe.getId(), recipe, callback);
    }

    /// get a recipe from the database
    /// @param rid the id of the recipe to get
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive the user object
    ///             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getRecipe(@NotNull final String rid, @NotNull final DatabaseCallback<Recipe> callback) {
        getData(RECIPES_PATH + "/" + rid, Recipe.class, callback);
    }

    /// get all the users from the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive a list of recipe objects
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see User
    public void getRecipeList(@NotNull final DatabaseCallback<List<Recipe>> callback) {
        getDataList(RECIPES_PATH, Recipe.class, callback);
    }

    /// delete a user from the database
    /// @param rid the recipe id to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteRecipe(@NotNull final String rid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(RECIPES_PATH + "/" + rid, callback);
    }

    public void updateRecipes(@NotNull final String recipeId, UnaryOperator<Recipe> function, @NotNull final DatabaseCallback<Recipe> callback) {
        runTransaction(RECIPES_PATH + "/" + recipeId, Recipe.class, function, callback);
    }


    // endregion Recipes Section

    // region favorite Section

    /// generate a new id for a new recipe in the database
    /// @return a new id for the user
    /// @see #generateNewId(String)
    /// @see User
    public String generateFavoriteRecipeId() {
        return generateNewId(FAVORITES_PATH);
    }

    /// create a new recipe in the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive void
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void createNewFavoriteRecipe(@NotNull final FavoriteRecipe favoriteRecipe, @Nullable final DatabaseCallback<Void> callback) {
        writeData(FAVORITES_PATH + "/" + favoriteRecipe.getId(), favoriteRecipe, callback);
    }

    /// get a recipe from the database
    /// @param id the id of the recipe to get
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive the user object
    ///             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getFavoriteRecipe(@NotNull final String id, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
        getData(FAVORITES_PATH + "/" + id, FavoriteRecipe.class, callback);
    }

    /// get all the users from the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive a list of recipe objects
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see User
    public void getFavoriteRecipeList(@NotNull final DatabaseCallback<List<FavoriteRecipe>> callback) {
        getDataList(FAVORITES_PATH, FavoriteRecipe.class, callback);
    }

    /// delete a user from the database
    /// @param callback the callback to call when the operation is completed
    public void deleteFavoriteRecipe(@NotNull final String id, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(FAVORITES_PATH + "/" + id, callback);
    }

    public void updateFavoriteRecipes(@NotNull final String id, UnaryOperator<FavoriteRecipe> function, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
        runTransaction(FAVORITES_PATH + "/" + id, FavoriteRecipe.class, function, callback);
    }


    public void getFavoriteRecipeByUserAndRecipe(@NotNull final String uid, @NotNull final String rid, @NotNull final DatabaseCallback<FavoriteRecipe> callback) {
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
            public void onFailed(Exception e) {

            }
        });
    }

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
            public void onFailed(Exception e) {

            }
        });
    }


    // endregion favorite Section
}