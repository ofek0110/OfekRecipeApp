package com.example.ofek.models;

import java.io.Serializable;

public class FoodTokPost implements Serializable {
    private String mediaUrl;
    private String mediaType; // "video" or "image"
    private String recipeTitle;
    private String chefName;

    public FoodTokPost() {
        // Required for Firebase
    }

    public FoodTokPost(String mediaUrl, String mediaType, String recipeTitle, String chefName) {
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.recipeTitle = recipeTitle;
        this.chefName = chefName;
    }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getRecipeTitle() { return recipeTitle; }
    public void setRecipeTitle(String recipeTitle) { this.recipeTitle = recipeTitle; }

    public String getChefName() { return chefName; }
    public void setChefName(String chefName) { this.chefName = chefName; }
}
