package com.example.ofek.models;

import com.google.firebase.database.Exclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Recipe implements Serializable {
    private String id;
    private String title;
    private String description;
    private String ingredients;
    private String instructions;
    private String imageBase64;
    private String userId;
    private String category;
    private String preparationTime;
    private String difficulty;
    private boolean isApproved;
    private String adminNotes;

    // המשתנה החדש: שומר מי דירג כדי שלא ידרגו פעמיים (מפתח: מזהה משתמש, ערך: הדירוג)
    private Map<String, Float> raters;

    public Recipe() {
        this.raters = new HashMap<>();
    }

    public Recipe(String id, String title, String description,
                  String ingredients, String instructions,
                  String imageBase64, String userId, String category,
                  String preparationTime, String difficulty,
                  boolean isApproved, String adminNotes,
                  Map<String, Float> raters) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.imageBase64 = imageBase64;
        this.userId = userId;
        this.category = category;
        this.preparationTime = preparationTime;
        this.difficulty = difficulty;
        this.isApproved = isApproved;
        this.adminNotes = adminNotes;
        this.raters = raters != null ? raters : new HashMap<>();
    }

    // --- Getters and Setters ---
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
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPreparationTime() { return preparationTime; }
    public void setPreparationTime(String preparationTime) { this.preparationTime = preparationTime; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { this.isApproved = approved; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public Map<String, Float> getRaters() { return raters; }
    public void setRaters(Map<String, Float> raters) { this.raters = raters; }
    public void putRater(String userId, float rating) {
        if (raters == null) {
            raters = new HashMap<>();
        }
        raters.put(userId, rating);
    }

    public void removeRater(String userId) {
        if (raters != null) {
            raters.remove(userId);
        }
    }

    @Exclude
    public boolean isPending() {
        return !isApproved && (adminNotes == null || adminNotes.isEmpty());
    }
    @Exclude
    public boolean isRejected() {
        return !isApproved && (adminNotes != null && !adminNotes.isEmpty());
    }

    // calculate the average rating of the recipe
    @Exclude
    public double getRating() {
        return raters.values().stream().mapToDouble(value -> value).average().orElse(0);
    }

    // calculate the total number of raters
    @Exclude
    public int getTotalRaters() {
        return raters.size();
    }
}