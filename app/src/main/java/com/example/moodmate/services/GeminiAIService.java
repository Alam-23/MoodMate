package com.example.moodmate.services;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiAIService {
    // Simple hardcoded API Key
    private static final String API_KEY = "AIzaSyCTTEvj2QzHx8Gph9-FM128sBOYtrJex94";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    
    private OkHttpClient client;
    private Gson gson;
    
    public interface AICallback {
        void onSuccess(String response, String moodAnalysis);
        void onError(String error);
    }
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public GeminiAIService(Context context) {
        android.util.Log.d("GeminiAIService", "Initializing Gemini AI Service");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        
        // Check network connectivity
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        android.util.Log.d("GeminiAIService", "Network connected: " + isConnected);
    }
    
    public void sendMessage(String userMessage, ChatCallback callback) {
        String prompt = createEmpathicPrompt(userMessage);
        
        JsonObject requestBody = createRequestBody(prompt);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Koneksi gagal: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Error HTTP: " + response.code());
                    return;
                }
                
                String responseBody = response.body().string();
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("candidates") && 
                        jsonResponse.getAsJsonArray("candidates").size() > 0) {
                        
                        JsonObject candidate = jsonResponse.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject();
                        
                        if (candidate.has("content")) {
                            JsonObject content = candidate.getAsJsonObject("content");
                            if (content.has("parts")) {
                                JsonArray parts = content.getAsJsonArray("parts");
                                if (parts.size() > 0) {
                                    JsonObject part = parts.get(0).getAsJsonObject();
                                    if (part.has("text")) {
                                        String aiResponse = part.get("text").getAsString();
                                        callback.onSuccess(aiResponse);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    
                    callback.onError("Response format tidak valid");
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }
    
    public void sendMessage(String userMessage, AICallback callback) {
        String prompt = createEmpathicPrompt(userMessage);
        
        JsonObject requestBody = createRequestBody(prompt);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Koneksi gagal: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Error HTTP: " + response.code());
                    return;
                }
                
                String responseBody = response.body().string();
                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (jsonResponse.has("candidates") && 
                        jsonResponse.getAsJsonArray("candidates").size() > 0) {
                        
                        JsonObject candidate = jsonResponse.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject();
                        JsonObject content = candidate.getAsJsonObject("content");
                        JsonArray parts = content.getAsJsonArray("parts");
                        
                        if (parts.size() > 0) {
                            String aiResponse = parts.get(0).getAsJsonObject()
                                    .get("text").getAsString();
                            
                            // Extract mood analysis if available
                            String moodAnalysis = extractMoodAnalysis(aiResponse);
                            
                            callback.onSuccess(aiResponse, moodAnalysis);
                        } else {
                            callback.onError("Respons tidak valid");
                        }
                    } else {
                        callback.onError("Tidak ada respons dari AI");
                    }
                } catch (Exception e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
    
    private String createEmpathicPrompt(String userMessage) {
        return "Kamu adalah asisten AI yang empatik dan penuh perhatian bernama MoodMate. " +
               "Tugasmu adalah mendengarkan cerita pengguna dengan empati, memberikan respons yang hangat dan mendukung, " +
               "serta menganalisis mood mereka. " +
               "Berikan respons yang menunjukkan pemahaman dan empati terhadap perasaan pengguna. " +
               "Di akhir respons, berikan analisis mood dalam format: [MOOD_ANALYSIS: mood_detected] " +
               "dimana mood_detected bisa berupa: happy, sad, anxious, angry, neutral, excited, stressed, calm. " +
               "Pesan pengguna: \"" + userMessage + "\"";
    }
    
    private JsonObject createRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        
        // Create contents array
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        
        content.add("parts", parts);
        contents.add(content);
        
        requestBody.add("contents", contents);
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("topP", 0.8);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("maxOutputTokens", 1024);
        
        requestBody.add("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    private String extractMoodAnalysis(String response) {
        if (response.contains("[MOOD_ANALYSIS:")) {
            int startIndex = response.indexOf("[MOOD_ANALYSIS:") + 15;
            int endIndex = response.indexOf("]", startIndex);
            if (endIndex > startIndex) {
                return response.substring(startIndex, endIndex).trim();
            }
        }
        return null;
    }
    
    // Method untuk analisis mood dari text
    public String analyzeMood(String text) {
        // Simple mood analysis based on keywords
        String lowercaseText = text.toLowerCase();
        String result;
        
        if (lowercaseText.contains("sedih") || lowercaseText.contains("kecewa") || 
            lowercaseText.contains("galau") || lowercaseText.contains("stress") ||
            lowercaseText.contains("down") || lowercaseText.contains("murung")) {
            result = "Sedih";
        } else if (lowercaseText.contains("senang") || lowercaseText.contains("bahagia") || 
                   lowercaseText.contains("gembira") || lowercaseText.contains("excited") ||
                   lowercaseText.contains("happy") || lowercaseText.contains("suka")) {
            result = "Senang";
        } else if (lowercaseText.contains("marah") || lowercaseText.contains("kesel") || 
                   lowercaseText.contains("benci") || lowercaseText.contains("jengkel") ||
                   lowercaseText.contains("emosi") || lowercaseText.contains("angry")) {
            result = "Marah";
        } else if (lowercaseText.contains("takut") || lowercaseText.contains("cemas") || 
                   lowercaseText.contains("khawatir") || lowercaseText.contains("panik") ||
                   lowercaseText.contains("anxious") || lowercaseText.contains("worry")) {
            result = "Cemas";
        } else {
            result = "Netral";
        }
        
        android.util.Log.d("GeminiAIService", "Text: '" + text + "' -> Mood: " + result);
        return result;
    }
    
    // Method to clear session/cache for different users
    public void clearSession() {
        android.util.Log.d("GeminiAIService", "Clearing AI service session");
        // Clear any cached data if needed
        // Recreate client to ensure clean session
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}