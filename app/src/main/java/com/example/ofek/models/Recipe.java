package com.example.ofek.models;

import java.io.Serializable;

public class Recipe implements Serializable {
    private String id;
    private String title;
    private String description;
    private String ingredients;
    private String instructions;
    private String imageUrl;
    private String userId;
    private String category;
    private String preparationTime;
    private String difficulty; // שדה חדש לרמת קושי
    private boolean isApproved;
    private String adminNotes;

    // בנאי ריק (חובה עבור Firebase)
    public Recipe() {
    }

    public Recipe(String id, String title, String description, String ingredients, String instructions, String userId, String category, String preparationTime, String difficulty) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.userId = userId;
        this.category = category;
        this.preparationTime = preparationTime;
        this.difficulty = difficulty;
        this.isApproved = false; // ברירת מחדל: לא מאושר עד שמנהל יאשר
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(String preparationTime) {
        this.preparationTime = preparationTime;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}