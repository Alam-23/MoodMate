package com.example.moodmate.models;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private long timestamp;
    private boolean isTyping;
    
    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.isTyping = false;
    }
    
    // Constructor for typing indicator
    public static ChatMessage createTypingMessage() {
        ChatMessage typingMessage = new ChatMessage("", false, System.currentTimeMillis());
        typingMessage.isTyping = true;
        return typingMessage;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isUser() {
        return isUser;
    }
    
    public void setUser(boolean user) {
        isUser = user;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isTyping() {
        return isTyping;
    }
    
    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}