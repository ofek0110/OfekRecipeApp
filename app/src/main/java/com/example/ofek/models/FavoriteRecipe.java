package com.example.ofek.models;

import androidx.annotation.NonNull;

public class FavoriteRecipe {


    private String id;
    private String recipeId;
    private String userId;

    public FavoriteRecipe() {
    }

    public FavoriteRecipe(String id, String recipeId, String userId) {
        this.id = id;
        this.recipeId = recipeId;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @NonNull

    @Override
    public String toString() {
        return "FavoriteRecipe{" +
                "id='" + id + '\'' +
                ", recipeId='" + recipeId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
