package com.example.ofek.models;

import androidx.annotation.NonNull;

// מודל נתונים שמייצג קשר של מתכון מועדף בבסיס הנתונים (Firebase)
public class FavoriteRecipe {

    // משתני המחלקה שיוצרים את הקישור
    private String id;        // מזהה ייחודי לרשומת המועדף עצמה
    private String recipeId;  // ה-ID של המתכון שסומן כאהוב
    private String userId;    // ה-ID של המשתמש שסימן את המתכון

    // קונסטרקטור ריק - חובה בשביל ש-Firebase ידע לשלוף ולפענח את הדאטה אוטומטית
    public FavoriteRecipe() {
    }

    // קונסטרקטור רגיל ליצירת אובייקט חדש עם כל הנתונים
    public FavoriteRecipe(String id, String recipeId, String userId) {
        this.id = id;
        this.recipeId = recipeId;
        this.userId = userId;
    }

    // גטרים וסטרים (Getters & Setters) לגישה ועדכון של המשתנים
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
    // פונקציית עזר להדפסת נתוני האובייקט בצורה קריאה ב-Logcat (בעיקר לדיבאג)
    public String toString() {
        return "FavoriteRecipe{" +
                "id='" + id + '\'' +
                ", recipeId='" + recipeId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}