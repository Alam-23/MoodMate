package com.example.moodmate.models;

public class ActivityRecommendation {
    private String icon;
    private String title;
    private String description;
    private String category;

    public ActivityRecommendation(String icon, String title, String description, String category) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }
}