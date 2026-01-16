package com.example.ofek.models;

import java.io.Serializable;

public class Recipe implements Serializable {
    private String id;
    private String title;
    private String description;
    private String ingredients;
    private String instructions;
    private String imageUrl;
    private String category;
    private String userId; // The creator of the recipe
    private boolean isApproved; // Status for admin approval
    private String adminNotes; // Notes from admin if rejected or for changes

    public Recipe() {
        // Required for Firebase
    }

    public Recipe(String id, String title, String description, String ingredients, String instructions, String imageUrl, String category, String userId, boolean isApproved) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.imageUrl = imageUrl;
        this.category = category;
        this.userId = userId;
        this.isApproved = isApproved;
        this.adminNotes = "";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
