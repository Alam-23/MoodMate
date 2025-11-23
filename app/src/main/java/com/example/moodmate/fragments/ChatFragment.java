package com.example.moodmate.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmate.MainActivity;
import com.example.moodmate.R;
import com.example.moodmate.adapters.ChatAdapter;
import com.example.moodmate.database.DatabaseHelper;
import com.example.moodmate.models.ChatMessage;
import com.example.moodmate.models.MoodEntry;
import com.example.moodmate.services.GeminiAIService;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private GeminiAIService aiService;
    private DatabaseHelper databaseHelper;
    private int userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        
        aiService = new GeminiAIService(getContext());
        databaseHelper = new DatabaseHelper(getContext());
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MoodMatePrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        android.util.Log.d("ChatFragment", "Retrieved userId from SharedPreferences: " + userId);
        
        // Clear AI service session for clean start
        aiService.clearSession();
        
        // Test database connection
        databaseHelper.testDatabase();
        
        // Load previous messages from database
        loadChatHistory();
        
        // Add welcome message if no previous messages
        if (chatMessages.isEmpty()) {
            addWelcomeMessage();
        }
        
        return view;
    }
    
    private void initViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
    }
    
    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        
        // Auto-scroll to bottom when keyboard shows
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !chatMessages.isEmpty()) {
                chatRecyclerView.post(() -> {
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                });
            }
        });
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;
        
        // Check internet connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Tidak ada koneksi internet. Periksa jaringan Anda.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Add user message
        ChatMessage userMessage = new ChatMessage(message, true, System.currentTimeMillis());
        chatMessages.add(userMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        
        // Save user message to database
        databaseHelper.insertChatMessage(userMessage, userId);
        
        messageInput.setText("");
        
        // Send to AI
        aiService.sendMessage(message, new GeminiAIService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Add AI response
                        ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
                        chatMessages.add(aiMessage);
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        scrollToBottom();
                        
                        // Save AI message to database
                        databaseHelper.insertChatMessage(aiMessage, userId);
                        
                        // Simple mood analysis from user message
                        String analyzedMood = aiService.analyzeMood(message);
                        android.util.Log.d("ChatFragment", "Original message: " + message);
                        android.util.Log.d("ChatFragment", "Analyzed mood: " + analyzedMood);
                        
                        // Special command for testing multiple moods
                        if (message.toLowerCase().contains("test mood data")) {
                            // Trigger test data creation
                            createTestMoodData();
                            return; // Don't process normal mood analysis
                        }
                        
                        if (analyzedMood != null) {
                            MoodEntry moodEntry = new MoodEntry(
                                analyzedMood,
                                MoodEntry.getMoodScore(analyzedMood),
                                "chat",
                                System.currentTimeMillis()
                            );
                            long result = databaseHelper.insertMoodEntry(moodEntry, userId);
                            android.util.Log.d("ChatFragment", "Mood saved with ID: " + result);
                            
                            // Notify MainActivity about mood update
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).notifyMoodUpdate();
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.e("ChatFragment", "AI Service Error: " + error);
                        
                        String userFriendlyMessage;
                        if (error.contains("Koneksi gagal") || error.contains("Network")) {
                            userFriendlyMessage = "Koneksi internet bermasalah. Periksa jaringan Anda dan coba lagi.";
                        } else if (error.contains("HTTP")) {
                            userFriendlyMessage = "Server sedang sibuk. Silakan coba beberapa saat lagi.";
                        } else {
                            userFriendlyMessage = "Maaf, terjadi kesalahan. Silakan coba lagi.";
                        }
                        
                        Toast.makeText(getContext(), userFriendlyMessage, Toast.LENGTH_LONG).show();
                        chatMessages.add(new ChatMessage(userFriendlyMessage, false, System.currentTimeMillis()));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        scrollToBottom();
                    });
                }
            }
        });
    }
    
    private void addWelcomeMessage() {
        String welcomeMsg = "Halo! Saya di sini untuk mendengarkan cerita Anda. Bagaimana perasaan Anda hari ini? Ceritakan apa yang ada di pikiran Anda, saya akan mendengarkan dengan empati dan membantu menganalisis mood Anda. 😊";
        chatMessages.add(new ChatMessage(welcomeMsg, false, System.currentTimeMillis()));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }
    

    
    private void scrollToBottom() {
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.post(() -> {
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            });
        }
    }
    
    private void loadChatHistory() {
        chatMessages.clear();
        List<ChatMessage> history = databaseHelper.getAllChatMessages(userId);
        chatMessages.addAll(history);
        chatAdapter.notifyDataSetChanged();
        if (!chatMessages.isEmpty()) {
            scrollToBottom();
        }
    }
    
    private void createTestMoodData() {
        long currentTime = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000;
        long oneDay = 24 * oneHour;
        
        // Clear existing user data
        databaseHelper.clearUserData(userId);
        
        // Create multiple moods for today
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 8.0f, "chat", currentTime - (4 * oneHour)), userId); // 4 hours ago
        databaseHelper.insertMoodEntry(new MoodEntry("Netral", 5.5f, "chat", currentTime - (2 * oneHour)), userId); // 2 hours ago  
        databaseHelper.insertMoodEntry(new MoodEntry("Cemas", 3.5f, "chat", currentTime - oneHour), userId);        // 1 hour ago
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 7.0f, "chat", currentTime - (oneHour/2)), userId);   // 30 min ago
        
        // Yesterday - multiple entries
        databaseHelper.insertMoodEntry(new MoodEntry("Sedih", 2.0f, "chat", currentTime - oneDay - (6 * oneHour)), userId);
        databaseHelper.insertMoodEntry(new MoodEntry("Netral", 5.0f, "chat", currentTime - oneDay - (3 * oneHour)), userId);
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 8.5f, "chat", currentTime - oneDay - oneHour), userId);
        
        // 2 days ago
        databaseHelper.insertMoodEntry(new MoodEntry("Marah", 1.5f, "chat", currentTime - (2 * oneDay) - (4 * oneHour)), userId);
        databaseHelper.insertMoodEntry(new MoodEntry("Cemas", 3.0f, "chat", currentTime - (2 * oneDay) - oneHour), userId);
        
        // 3 days ago
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 8.0f, "chat", currentTime - (3 * oneDay)), userId);
        
        // Add response message
        ChatMessage testResponse = new ChatMessage("✅ Test data berhasil dibuat! Sekarang coba buka tab Mood untuk melihat grafik dengan multiple entries per hari.", false, System.currentTimeMillis());
        chatMessages.add(testResponse);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        
        // Save test response to database
        databaseHelper.insertChatMessage(testResponse, userId);
        
        android.util.Log.d("ChatFragment", "Test mood data created with multiple entries per day");
    }
    
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && 
                       (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }
}