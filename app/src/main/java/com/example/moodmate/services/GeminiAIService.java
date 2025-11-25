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
        
        // Handle inappropriate requests
        if (prompt.equals("REDIRECT_TO_MENTAL_HEALTH")) {
            String redirectResponse = "Maaf, sebagai MoodMate saya fokus membantu kesehatan mental Anda 💜\n\n" +
                    "Saya di sini untuk:\n" +
                    "✨ Mendengarkan cerita dan perasaan Anda\n" +
                    "💭 Membantu menganalisis mood dan emosi\n" +
                    "🤗 Memberikan dukungan dan motivasi\n" +
                    "📈 Tracking perkembangan mental wellness\n\n" +
                    "Ceritakan bagaimana perasaan Anda hari ini? 😊";
            callback.onSuccess(redirectResponse);
            return;
        }
        
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
                callback.onError("Koneksi bermasalah. Periksa internet Anda dan coba lagi 🌐");
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
                                        String cleanResponse = cleanAIResponse(aiResponse);
                                        callback.onSuccess(cleanResponse);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    
                    callback.onError("Maaf, saya sedang mengalami gangguan. Coba lagi dalam beberapa saat 😔");
                } catch (Exception e) {
                    callback.onError("Terjadi kesalahan saat memproses respons. Coba lagi ya 🔄");
                }
            }
        });
    }
    
    public void sendMessage(String userMessage, AICallback callback) {
        // Handle inappropriate requests
        if (isInappropriateRequest(userMessage)) {
            String redirectResponse = "Maaf, sebagai MoodMate saya fokus membantu kesehatan mental Anda 💜\\n\\n" +
                    "Saya di sini untuk:\\n" +
                    "✨ Mendengarkan cerita dan perasaan Anda\\n" +
                    "💭 Membantu menganalisis mood dan emosi\\n" +
                    "🤗 Memberikan dukungan dan motivasi\\n" +
                    "📈 Tracking perkembangan mental wellness\\n\\n" +
                    "Ceritakan bagaimana perasaan Anda hari ini? 😊";
            callback.onSuccess(redirectResponse, "Netral");
            return;
        }
        
        // Enhanced prompt with mood analysis request
        String prompt = "Anda adalah MoodMate, AI companion untuk kesehatan mental. " +
               "ATURAN KETAT:\\n" +
               "1. HANYA bicara tentang kesehatan mental, perasaan, emosi, dan dukungan psikologis\\n" +
               "2. Berikan respons yang empati, supportif, dan caring\\n" +
               "3. Analisis mood dari pesan user dan tentukan satu mood utama\\n" +
               "4. Mood yang valid: Senang, Sedih, Marah, Cemas, Excited, Stress, Calm\\n" +
               "5. Di akhir respons, tambahkan [MOOD_DETECTED: mood_name] untuk internal analysis\\n\\n" +
               "Pesan user: \\\"" + userMessage + "\\\"";
        
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
                // Fallback with local mood analysis
                String localMood = analyzeMood(userMessage);
                callback.onError("Koneksi bermasalah: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handleHttpError(response.code(), new ChatCallback() {
                        @Override
                        public void onSuccess(String response) {}
                        @Override 
                        public void onError(String error) {
                            // Fallback with local mood analysis
                            String localMood = analyzeMood(userMessage);
                            callback.onError(error);
                        }
                    });
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
                            
                            // Extract mood from AI response first, then clean response
                            String detectedMood = extractMoodFromAIResponse(aiResponse, userMessage);
                            String cleanResponse = cleanAIResponse(aiResponse);
                            
                            android.util.Log.d("GeminiAIService", "AI Response: " + aiResponse);
                            android.util.Log.d("GeminiAIService", "Detected Mood: " + detectedMood);
                            android.util.Log.d("GeminiAIService", "Clean Response: " + cleanResponse);
                            
                            callback.onSuccess(cleanResponse, detectedMood);
                        } else {
                            String localMood = analyzeMood(userMessage);
                            callback.onError("Respons tidak valid");
                        }
                    } else {
                        String localMood = analyzeMood(userMessage);
                        callback.onError("Tidak ada respons dari AI");
                    }
                } catch (Exception e) {
                    String localMood = analyzeMood(userMessage);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
    
    private String createEmpathicPrompt(String userMessage) {
        // Check if request is inappropriate first
        if (isInappropriateRequest(userMessage)) {
            return "REDIRECT_TO_MENTAL_HEALTH";
        }
        
        return "Anda adalah MoodMate, AI companion untuk kesehatan mental. " +
               "ATURAN KETAT:\n" +
               "1. HANYA bicara tentang kesehatan mental, perasaan, emosi, dan dukungan psikologis\n" +
               "2. TIDAK BOLEH membantu coding, programming, atau technical questions\n" +
               "3. TIDAK BOLEH menjawab pertanyaan diluar konteks kesehatan mental\n" +
               "4. TIDAK BOLEH memberikan kode, script, atau solusi teknis\n" +
               "5. Jika ditanya hal diluar konteks, arahkan kembali ke kesehatan mental\n" +
               "6. Berikan respons yang empati, supportif, dan caring\n" +
               "7. JANGAN gunakan format [MOOD_ANALYSIS] atau [MOOD_ONLY] dalam respons\n" +
               "8. Respons harus natural dan conversational\n\n" +
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
    
    private String extractMoodFromAIResponse(String aiResponse, String userMessage) {
        // First try to extract from AI response marker
        if (aiResponse.contains("[MOOD_DETECTED:") && aiResponse.contains("]")) {
            int startIndex = aiResponse.indexOf("[MOOD_DETECTED:") + 15;
            int endIndex = aiResponse.indexOf("]", startIndex);
            if (endIndex > startIndex) {
                String mood = aiResponse.substring(startIndex, endIndex).trim();
                android.util.Log.d("GeminiAIService", "Extracted mood from marker: " + mood);
                return validateMood(mood);
            }
        }
        
        // Try legacy marker format
        if (aiResponse.contains("[MOOD_ANALYSIS:")) {
            int startIndex = aiResponse.indexOf("[MOOD_ANALYSIS:") + 15;
            int endIndex = aiResponse.indexOf("]", startIndex);
            if (endIndex > startIndex) {
                String mood = aiResponse.substring(startIndex, endIndex).trim();
                android.util.Log.d("GeminiAIService", "Extracted mood from legacy marker: " + mood);
                return validateMood(mood);
            }
        }
        
        // Fallback to analyzing user message directly
        String analyzedMood = analyzeMood(userMessage);
        android.util.Log.d("GeminiAIService", "Fallback mood analysis: " + analyzedMood);
        return analyzedMood;
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
    
    // Enhanced method untuk analisis mood dari text
    public String analyzeMood(String text) {
        String lowercaseText = text.toLowerCase();
        
        // Enhanced mood detection keywords with more variations
        if (lowercaseText.contains("sedih") || lowercaseText.contains("menangis") || 
            lowercaseText.contains("duka") || lowercaseText.contains("terpuruk") ||
            lowercaseText.contains("putus asa") || lowercaseText.contains("hancur") ||
            lowercaseText.contains("kecewa") || lowercaseText.contains("galau") ||
            lowercaseText.contains("down") || lowercaseText.contains("murung") ||
            lowercaseText.contains("patah hati") || lowercaseText.contains("larut")) {
            return "Sedih";
        } else if (lowercaseText.contains("senang") || lowercaseText.contains("gembira") || 
                  lowercaseText.contains("bahagia") || lowercaseText.contains("suka cita") ||
                  lowercaseText.contains("riang") || lowercaseText.contains("ceria") ||
                  lowercaseText.contains("happy") || lowercaseText.contains("excited") ||
                  lowercaseText.contains("suka") || lowercaseText.contains("enjoy")) {
            return "Senang";
        } else if (lowercaseText.contains("marah") || lowercaseText.contains("kesal") || 
                  lowercaseText.contains("jengkel") || lowercaseText.contains("benci") ||
                  lowercaseText.contains("murka") || lowercaseText.contains("dongkol") ||
                  lowercaseText.contains("emosi") || lowercaseText.contains("angry") ||
                  lowercaseText.contains("sebel") || lowercaseText.contains("geram")) {
            return "Marah";
        } else if (lowercaseText.contains("cemas") || lowercaseText.contains("khawatir") || 
                  lowercaseText.contains("takut") || lowercaseText.contains("gelisah") ||
                  lowercaseText.contains("panik") || lowercaseText.contains("was-was") ||
                  lowercaseText.contains("anxious") || lowercaseText.contains("worry") ||
                  lowercaseText.contains("nervous") || lowercaseText.contains("deg-degan")) {
            return "Cemas";
        } else if (lowercaseText.contains("excited") || lowercaseText.contains("antusias") || 
                  lowercaseText.contains("semangat") || lowercaseText.contains("bersemangat") ||
                  lowercaseText.contains("energik") || lowercaseText.contains("passionate")) {
            return "Excited";
        } else if (lowercaseText.contains("stress") || lowercaseText.contains("tertekan") || 
                  lowercaseText.contains("lelah") || lowercaseText.contains("capek") ||
                  lowercaseText.contains("burnout") || lowercaseText.contains("penat") ||
                  lowercaseText.contains("overwhelmed") || lowercaseText.contains("pressure")) {
            return "Stress";
        } else if (lowercaseText.contains("tenang") || lowercaseText.contains("damai") || 
                  lowercaseText.contains("rileks") || lowercaseText.contains("santai") ||
                  lowercaseText.contains("kalem") || lowercaseText.contains("nyaman") ||
                  lowercaseText.contains("peaceful") || lowercaseText.contains("calm")) {
            return "Calm";
        }
        
        android.util.Log.d("GeminiAIService", "Text: '" + text + "' -> Mood: Netral (no keywords matched)");
        return "Netral";
    }
    
    // Check if request is inappropriate (coding, off-topic, etc.)
    private boolean isInappropriateRequest(String message) {
        String lowerMessage = message.toLowerCase().trim();
        
        // Programming/coding keywords
        String[] codingKeywords = {
            "code", "coding", "program", "script", "html", "css", "javascript", "python", 
            "java", "android", "xml", "json", "api", "database", "sql", "git", "github",
            "function", "method", "class", "variable", "loop", "array", "object",
            "debug", "error", "compile", "syntax", "algorithm", "framework", "library",
            "import", "package", "extends", "implements", "public", "private", "static"
        };
        
        // Off-topic keywords  
        String[] offTopicKeywords = {
            "weather", "cuaca", "recipe", "resep", "game", "sport", "olahraga", 
            "movie", "film", "music", "lagu", "news", "berita", "politics", "politik",
            "math", "matematika", "physics", "fisika", "history", "sejarah",
            "translate", "terjemah", "currency", "mata uang", "shopping", "belanja",
            "recipe", "masak", "travel", "wisata", "hotel", "restaurant"
        };
        
        // Check for inappropriate content
        for (String keyword : codingKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        for (String keyword : offTopicKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        // Check for code patterns
        if (lowerMessage.contains("{") || lowerMessage.contains("}") || 
            lowerMessage.contains("function(") || lowerMessage.contains("class ") ||
            lowerMessage.contains("import ") || lowerMessage.contains("package ") ||
            lowerMessage.contains("<?") || lowerMessage.contains("/>") ||
            lowerMessage.contains("SELECT ") || lowerMessage.contains("INSERT ") ||
            lowerMessage.contains("CREATE TABLE") || lowerMessage.contains("<!DOCTYPE")) {
            return true;
        }
        
        return false;
    }
    
    // Clean AI response from mood analysis markers
    private String cleanAIResponse(String response) {
        String cleanResponse = response;
        
        // Remove all mood analysis markers and patterns
        cleanResponse = cleanResponse.replaceAll("(?i)\\[MOOD_DETECTED:[^\\]]*\\]", "");
        cleanResponse = cleanResponse.replaceAll("(?i)\\[MOOD_ANALYSIS:[^\\]]*\\]", "");
        cleanResponse = cleanResponse.replaceAll("(?i)\\[?MOOD[_\\s]*ANALYSIS[_\\s]*:?[^\\]]*\\]?", "");
        cleanResponse = cleanResponse.replaceAll("(?i)\\[?MOOD[_\\s]*ONLY[_\\s]*:?[^\\]]*\\]?", "");
        cleanResponse = cleanResponse.replaceAll("(?i)MOOD[_\\s]*DETECTED[_\\s]*:?[^\\n]*", "");
        cleanResponse = cleanResponse.replaceAll("(?i)\\*\\*MOOD[^\\*]*\\*\\*", "");
        cleanResponse = cleanResponse.replaceAll("(?i)Mood\\s*terdeteksi[^\\n]*", "");
        
        // Remove empty lines at start and end
        cleanResponse = cleanResponse.replaceAll("^\\n+", "");
        cleanResponse = cleanResponse.replaceAll("\\n+$", "");
        
        // Clean up multiple newlines and extra spaces
        cleanResponse = cleanResponse.replaceAll("\\n{3,}", "\\n\\n");
        cleanResponse = cleanResponse.replaceAll("^\\s+", "");
        cleanResponse = cleanResponse.replaceAll("\\s+$", "");
        
        // If response is empty or too short after cleaning, provide default
        if (cleanResponse.trim().length() < 10) {
            cleanResponse = "Terima kasih sudah berbagi dengan saya. Saya mendengar Anda 💜\\n\\n" +
                          "Bagaimana perasaan Anda setelah menceritakan hal ini? " +
                          "Saya di sini untuk mendukung Anda 🤗";
        }
        
        return cleanResponse.trim();
    }
    
    // Handle HTTP errors with user-friendly messages
    private void handleHttpError(int statusCode, ChatCallback callback) {
        String errorMessage;
        switch (statusCode) {
            case 429:
                errorMessage = "Terlalu banyak request. Tunggu sebentar lalu coba lagi ⏱️";
                break;
            case 400:
                errorMessage = "Request tidak valid. Coba dengan pesan yang berbeda 🔄";
                break;
            case 403:
                errorMessage = "Akses ditolak. Periksa konfigurasi API 🔐";
                break;
            case 500:
            case 502:
            case 503:
                errorMessage = "Server sedang bermasalah. Coba lagi dalam beberapa menit ⚠️";
                break;
            default:
                errorMessage = "Terjadi kesalahan (" + statusCode + "). Coba lagi ya 🔄";
        }
        callback.onError(errorMessage);
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