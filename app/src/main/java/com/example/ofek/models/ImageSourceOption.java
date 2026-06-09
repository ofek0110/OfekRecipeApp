package com.example.ofek.models;

// מודל נתונים שמייצג אופציה לבחירת מקור תמונה (כמו מצלמה או גלריה) ב-Bottom Sheet
public class ImageSourceOption {

    // משתני המחלקה שיאכלסו את פרטי האופציה
    private String title;         // כותרת האופציה (למשל: "Take Photo")
    private String description;   // תיאור קצר (למשל: "Use camera to take a picture")
    private int iconResource;     // מזהה ה-ID של האייקון מתיקיית ה-drawable

    // קונסטרקטור רגיל לבניית האובייקט עם כל הנתונים
    public ImageSourceOption(String title, String description, int iconResource) {
        this.title = title;
        this.description = description;
        this.iconResource = iconResource;
    }

    // גטרים וסטרים (Getters & Setters) רגילים לגישה ועדכון השדות
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

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }
}