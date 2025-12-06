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
    private static final String API_KEY = "your key";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    
    private OkHttpClient client;
    private Gson gson;
    
    public interface ChatCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface MoodAnalysisCallback {
        void onSuccess(String mood, float score);
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
                callback.onError("Koneksi bermasalah. Periksa internet Anda dan coba lagi ");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handleHttpError(response.code(), callback);
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
                                        callback.onSuccess(aiResponse.trim());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    
                    callback.onError("Maaf, saya sedang mengalami gangguan. Coba lagi dalam beberapa saat");
                } catch (Exception e) {
                    callback.onError("Terjadi kesalahan saat memproses respons. Coba lagi ya ");
                }
            }
        });
    }
    

    
    private String createEmpathicPrompt(String userMessage) {
        return "Anda adalah MoodMate, AI companion untuk kesehatan mental. " +
               "ATURAN KETAT:\n" +
               "1. HANYA bicara tentang kesehatan mental, perasaan, emosi, dan dukungan psikologis\n" +
               "2. TIDAK BOLEH membantu coding, programming, atau technical questions\n" +
               "3. TIDAK BOLEH menjawab pertanyaan diluar konteks kesehatan mental\n" +
               "4. TIDAK BOLEH memberikan kode, script, atau solusi teknis\n" +
               "5. Jika ditanya hal diluar konteks, arahkan kembali ke kesehatan mental\n" +
               "6. Berikan respons yang empati, supportif, dan caring\n" +
               "7. Respons harus natural dan conversational\n\n" +
               "Pesan user: \"" + userMessage + "\"";
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
    
    // Handle HTTP errors with user-friendly messages
    private void handleHttpError(int statusCode, ChatCallback callback) {
        String errorMessage;
        switch (statusCode) {
            case 429:
                errorMessage = "Terlalu banyak request. Tunggu sebentar lalu coba lagi ⏱";
                break;
            case 400:
                errorMessage = "Request tidak valid. Coba dengan pesan yang berbeda ";
                break;
            case 403:
                errorMessage = "Akses ditolak. Periksa konfigurasi API ";
                break;
            case 500:
            case 502:
            case 503:
                errorMessage = "Server sedang bermasalah. Coba lagi dalam beberapa menit ";
                break;
            default:
                errorMessage = "Terjadi kesalahan (" + statusCode + "). Coba lagi ya ";
        }
        callback.onError(errorMessage);
    }
    
    // New method for mood analysis specifically
    public void sendMessageForMoodAnalysis(String prompt, MoodAnalysisCallback callback) {
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
                callback.onError("Koneksi bermasalah. Periksa internet Anda 🌐");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Layanan AI sedang bermasalah (" + response.code() + ")");
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
                            if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                                String aiResponse = content.getAsJsonArray("parts")
                                        .get(0).getAsJsonObject()
                                        .get("text").getAsString();
                                
                                // Parse AI response for mood and score
                                parseMoodFromAIResponse(aiResponse, callback);
                                return;
                            }
                        }
                    }
                    
                    callback.onError("Respons AI tidak valid");
                } catch (Exception e) {
                    callback.onError("Gagal memproses respons AI: " + e.getMessage());
                }
            }
        });
    }
    
    private void parseMoodFromAIResponse(String aiResponse, MoodAnalysisCallback callback) {
        try {
            // Try to find JSON in the response
            String jsonPart = aiResponse;
            
            // Extract JSON if it's wrapped in text
            if (aiResponse.contains("{") && aiResponse.contains("}")) {
                int startIndex = aiResponse.indexOf("{");
                int endIndex = aiResponse.lastIndexOf("}") + 1;
                jsonPart = aiResponse.substring(startIndex, endIndex);
            }
            
            // Parse JSON
            JsonObject moodJson = gson.fromJson(jsonPart, JsonObject.class);
            
            if (moodJson.has("mood") && moodJson.has("score")) {
                String mood = moodJson.get("mood").getAsString();
                float score = moodJson.get("score").getAsFloat();
                
                // Validate mood
                String validatedMood = validateMood(mood);
                float validatedScore = Math.max(1.0f, Math.min(10.0f, score));
                
                android.util.Log.d("GeminiAIService", "AI Mood Analysis - Mood: " + validatedMood + ", Score: " + validatedScore);
                callback.onSuccess(validatedMood, validatedScore);
            } else {
                callback.onError("Format respons AI tidak sesuai");
            }
            
        } catch (Exception e) {
            android.util.Log.e("GeminiAIService", "Error parsing AI response: " + e.getMessage());
            callback.onError("Gagal memproses respons AI");
        }
    }
    
    private String validateMood(String mood) {
        String[] validMoods = {"Senang", "Sedih", "Marah", "Cemas", "Excited", "Stress", "Calm", "Netral"};
        for (String validMood : validMoods) {
            if (validMood.equalsIgnoreCase(mood.trim())) {
                return validMood;
            }
        }
        return "Netral";
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