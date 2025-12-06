package com.example.moodmate.models;

public class MoodEntry {
    private String moodType;
    private float moodScore;
    private String source;
    private long timestamp;
    private String note;
    
    public MoodEntry(String moodType, float moodScore, String source, long timestamp) {
        this.moodType = moodType;
        this.moodScore = moodScore;
        this.source = source;
        this.timestamp = timestamp;
        this.note = "";
    }
    
    public MoodEntry(String moodType, float moodScore, String source, long timestamp, String note) {
        this.moodType = moodType;
        this.moodScore = moodScore;
        this.source = source;
        this.timestamp = timestamp;
        this.note = note != null ? note : "";
    }
    
    // Getters
    public String getMoodType() {
        return moodType;
    }
    
    public float getMoodScore() {
        return moodScore;
    }
    
    public String getSource() {
        return source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Setters
    public void setMoodType(String moodType) {
        this.moodType = moodType;
    }
    
    public void setMoodScore(float moodScore) {
        this.moodScore = moodScore;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getNote() {
        return note != null ? note : "";
    }
    
    public void setNote(String note) {
        this.note = note != null ? note : "";
    }
    
    public static float getMoodScore(String moodType) {
        switch (moodType.toLowerCase()) {
            case "senang":
            case "happy":
            case "bahagia":
            case "gembira":
                return 8.5f;
            case "excited":
                return 9.0f;
            case "calm":
                return 7.0f;
            case "netral":
            case "neutral":
                return 5.0f;
            case "cemas":
            case "anxious":
            case "khawatir":
            case "takut":
                return 3.0f;
            case "stress":
            case "stressed":
                return 2.5f;
            case "sedih":
            case "sad":
            case "kecewa":
            case "galau":
                return 2.0f;
            case "marah":
            case "angry":
            case "kesal":
            case "jengkel":
                return 1.5f;
            default:
                return 5.0f;
        }
    }
}