package com.example.moodmate.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmate.services.GeminiAIService;

import com.example.moodmate.R;
import com.example.moodmate.database.DatabaseHelper;
import com.example.moodmate.models.ActivityRecommendation;
import com.example.moodmate.models.MoodEntry;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodFragment extends Fragment {
    private LineChart moodChart;
    private PieChart moodDistribution;
    private TextView currentMoodText;
    private TextView moodInsightText;
    private TextView emptyStateText;
    private androidx.cardview.widget.CardView singleRecommendationCard;
    private TextView singleRecommendationIcon;
    private TextView singleRecommendationTitle;
    private TextView singleRecommendationDescription;
    private DatabaseHelper databaseHelper;
    private GeminiAIService geminiAIService;
    private int userId;
    
    // Mood input components
    private EditText moodDescriptionInput;
    private Button submitMoodBtn;
    private String detectedMood = "";
    private float detectedMoodScore = 0.0f;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood, container, false);
        
        initViews(view);
        setupMoodInput(view);
        databaseHelper = new DatabaseHelper(getContext());
        geminiAIService = new GeminiAIService(getContext());
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MoodMatePrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        android.util.Log.d("MoodFragment", "Retrieved userId from SharedPreferences: " + userId);
        
        loadMoodData();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh mood data when fragment becomes visible
        refreshMoodData();
    }
    
    // Public method to refresh mood data from other fragments
    public void refreshMoodData() {
        if (databaseHelper != null) {
            loadMoodData();
            android.util.Log.d("MoodFragment", "Mood data refreshed");
        }
    }
    
    // This method is now handled by refreshMoodData()
    
    // Method untuk testing multiple moods per day
    public void addTestData() {
        if (databaseHelper == null) return;
        
        long currentTime = System.currentTimeMillis();
        long oneDayMs = 24 * 60 * 60 * 1000;
        
        // Today - multiple moods
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 8.0f, "chat", currentTime - 3600000), userId); // 1 hour ago
        databaseHelper.insertMoodEntry(new MoodEntry("Netral", 5.5f, "chat", currentTime - 1800000), userId); // 30 min ago
        databaseHelper.insertMoodEntry(new MoodEntry("Cemas", 3.0f, "chat", currentTime - 900000), userId);  // 15 min ago
        
        // Yesterday - multiple moods
        databaseHelper.insertMoodEntry(new MoodEntry("Sedih", 2.5f, "chat", currentTime - oneDayMs - 7200000), userId); // Yesterday morning
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 7.5f, "chat", currentTime - oneDayMs - 3600000), userId); // Yesterday afternoon
        
        // 2 days ago - single mood
        databaseHelper.insertMoodEntry(new MoodEntry("Marah", 1.5f, "chat", currentTime - (2 * oneDayMs)), userId);
        
        // 3 days ago - multiple moods
        databaseHelper.insertMoodEntry(new MoodEntry("Netral", 5.0f, "chat", currentTime - (3 * oneDayMs) - 5400000), userId); // Morning
        databaseHelper.insertMoodEntry(new MoodEntry("Senang", 8.5f, "chat", currentTime - (3 * oneDayMs) - 1800000), userId); // Evening
        
        android.util.Log.d("MoodFragment", "Test data added with multiple moods per day");
        
        // Refresh data
        loadMoodData();
    }
    
    private void initViews(View view) {
        moodChart = view.findViewById(R.id.mood_chart);
        moodDistribution = view.findViewById(R.id.mood_distribution);
        currentMoodText = view.findViewById(R.id.current_mood_text);
        moodInsightText = view.findViewById(R.id.mood_insight_text);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        singleRecommendationCard = view.findViewById(R.id.single_recommendation_card);
        singleRecommendationIcon = view.findViewById(R.id.single_recommendation_icon);
        singleRecommendationTitle = view.findViewById(R.id.single_recommendation_title);
        singleRecommendationDescription = view.findViewById(R.id.single_recommendation_description);
        
        // Debug log to check if views are found
        android.util.Log.d("MoodFragment", "initViews - recommendation card: " + (singleRecommendationCard != null) 
            + ", icon: " + (singleRecommendationIcon != null) 
            + ", title: " + (singleRecommendationTitle != null) 
            + ", description: " + (singleRecommendationDescription != null));
        android.util.Log.d("MoodFragment", "initViews - recommendation icon: " + (singleRecommendationIcon != null));
        android.util.Log.d("MoodFragment", "initViews - recommendation title: " + (singleRecommendationTitle != null));
        android.util.Log.d("MoodFragment", "initViews - recommendation description: " + (singleRecommendationDescription != null));
        
        // Don't setup recommendations initially - wait for mood data
    }
    
    private void setupMoodInput(View view) {
        // Initialize components
        moodDescriptionInput = view.findViewById(R.id.mood_description_input);
        submitMoodBtn = view.findViewById(R.id.submit_mood_btn);
        
        // Set up text watcher for enabling submit button
        moodDescriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                
                // Enable/disable submit button based on text input
                if (!text.isEmpty()) {
                    submitMoodBtn.setEnabled(true);
                    submitMoodBtn.setAlpha(1.0f);
                } else {
                    submitMoodBtn.setEnabled(false);
                    submitMoodBtn.setAlpha(0.5f);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Set up submit button to save mood directly
        submitMoodBtn.setOnClickListener(v -> {
            saveMoodEntry();
        });
    }
    
    private void saveMoodEntry() {
        String userInput = moodDescriptionInput.getText().toString().trim();
        
        if (userInput.isEmpty()) {
            Toast.makeText(getContext(), "Masukkan deskripsi mood terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button and show loading
        submitMoodBtn.setEnabled(false);
        submitMoodBtn.setText("Menganalisis...");
        
        // Use Gemini AI to analyze mood
        String moodAnalysisPrompt = createMoodAnalysisPrompt(userInput);
        
        geminiAIService.sendMessageForMoodAnalysis(moodAnalysisPrompt, new GeminiAIService.MoodAnalysisCallback() {
            @Override
            public void onSuccess(String mood, float score) {
                // Save to database
                MoodEntry moodEntry = new MoodEntry(mood, score, "manual", System.currentTimeMillis());
                moodEntry.setNote(userInput);
                
                long result = databaseHelper.insertMoodEntry(moodEntry, userId);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (result != -1) {
                            Toast.makeText(getContext(), "Mood berhasil dianalisis dan disimpan! 🎯", Toast.LENGTH_SHORT).show();
                            android.util.Log.d("MoodFragment", "AI detected mood: " + mood + " with score: " + score);
                            
                            // Reset form
                            resetMoodInput();
                            
                            // Refresh mood data
                            loadMoodData();
                        } else {
                            Toast.makeText(getContext(), "Gagal menyimpan mood", Toast.LENGTH_SHORT).show();
                            resetSubmitButton();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Gagal menganalisis mood: " + error, Toast.LENGTH_SHORT).show();
                        resetSubmitButton();
                    });
                }
            }
        });
    }
    
    private String createMoodAnalysisPrompt(String userInput) {
        return "Analisis mood dari text ini dan berikan hasilnya dalam format JSON sederhana:\\n\\n" +
               "Text: \\\"" + userInput + "\\\"\\n\\n" +
               "Berikan response dalam format:\\n" +
               "{\\\"mood\\\": \\\"[mood_name]\\\", \\\"score\\\": [1-10]}\\n\\n" +
               "Mood yang valid: Senang, Sedih, Marah, Cemas, Excited, Stress, Calm, Netral\\n" +
               "Score: 1-3 (sangat negatif), 4-6 (netral), 7-10 (positif)\\n\\n" +
               "Contoh: {\\\"mood\\\": \\\"Senang\\\", \\\"score\\\": 8.5}";
    }
    
    private void resetSubmitButton() {
        submitMoodBtn.setEnabled(true);
        submitMoodBtn.setText("Kirim");
        submitMoodBtn.setAlpha(1.0f);
    }
    
    private void resetMoodInput() {
        // Reset form state
        moodDescriptionInput.setText("");
        detectedMood = "";
        detectedMoodScore = 0.0f;
        
        // Reset submit button
        submitMoodBtn.setEnabled(false);
        submitMoodBtn.setText("Kirim");
        submitMoodBtn.setAlpha(0.5f);
    }
    
    private void setupCharts() {
        setupLineChart();
        setupPieChart();
    }
    
    private void setupLineChart() {
        moodChart.setBackgroundColor(Color.TRANSPARENT);
        
        // Setup left axis (mood score)
        YAxis leftAxis = moodChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setGranularity(1f);
        leftAxis.setLabelCount(11, true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.argb(50, 255, 255, 255));
        
        // Disable right axis
        moodChart.getAxisRight().setEnabled(false);
        
        // Setup X axis
        XAxis xAxis = moodChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.argb(50, 255, 255, 255));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        
        // Setup legend
        moodChart.getLegend().setTextColor(Color.WHITE);
        moodChart.getLegend().setEnabled(true);
        
        // Enable touch interactions
        moodChart.setTouchEnabled(true);
        moodChart.setDragEnabled(true);
        moodChart.setScaleEnabled(true);
        moodChart.setPinchZoom(true);
        moodChart.setDoubleTapToZoomEnabled(true);
        
        // Setup description
        Description desc = new Description();
        desc.setText("Grafik Tren Mood");
        desc.setTextColor(Color.WHITE);
        desc.setTextSize(12f);
        moodChart.setDescription(desc);
        
        // Set animation
        moodChart.animateX(1000);
    }
    
    private void setupPieChart() {
        moodDistribution.setBackgroundColor(Color.TRANSPARENT);
        moodDistribution.setHoleColor(Color.TRANSPARENT);
        moodDistribution.setTransparentCircleColor(Color.WHITE);
        moodDistribution.setTransparentCircleAlpha(110);
        moodDistribution.setHoleRadius(40f);
        moodDistribution.setTransparentCircleRadius(45f);
        moodDistribution.setDrawCenterText(true);
        moodDistribution.setCenterText("Distribusi\nMood");
        moodDistribution.setCenterTextColor(Color.WHITE);
        moodDistribution.setCenterTextSize(12f);
        
        Description desc = new Description();
        desc.setText("Distribusi Mood Mingguan");
        desc.setTextColor(Color.WHITE);
        moodDistribution.setDescription(desc);
        
        moodDistribution.getLegend().setTextColor(Color.WHITE);
    }
    
    private void loadMoodData() {
        try {
            android.util.Log.d("MoodFragment", "Loading mood data for userId: " + userId);
            List<MoodEntry> moodEntries = databaseHelper.getAllMoodEntries(userId);
            
            // Debug log
            android.util.Log.d("MoodFragment", "Loaded mood entries: " + moodEntries.size());
            for (MoodEntry entry : moodEntries) {
                android.util.Log.d("MoodFragment", "Mood: " + entry.getMoodType() + ", Score: " + entry.getMoodScore() + ", Time: " + new Date(entry.getTimestamp()));
            }
            
            if (moodEntries.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                setupCharts();
                loadRealLineData(moodEntries);
                loadRealPieData(moodEntries);
                updateCurrentMoodAndInsight(moodEntries);
                setupSingleRecommendation(); // Setup recommendation card
                updateSingleRecommendation(moodEntries);
            }
        } catch (Exception e) {
            android.util.Log.e("MoodFragment", "Error loading mood data: " + e.getMessage());
            showEmptyState();
        }
    }
    
    private void showEmptyState() {
        moodChart.setVisibility(View.GONE);
        moodDistribution.setVisibility(View.GONE);
        currentMoodText.setVisibility(View.GONE);
        moodInsightText.setVisibility(View.GONE);
        // Hide recommendation card when no mood data
        if (singleRecommendationCard != null) {
            singleRecommendationCard.setVisibility(View.GONE);
        }
        
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("💬 Mulai chat dengan AI untuk melacak mood Anda!\n\nMood tracking akan muncul setelah Anda berinteraksi dengan chatbot.");
        }
    }

    private void hideEmptyState() {
        moodChart.setVisibility(View.VISIBLE);
        moodDistribution.setVisibility(View.VISIBLE);
        currentMoodText.setVisibility(View.VISIBLE);
        moodInsightText.setVisibility(View.VISIBLE);
        singleRecommendationCard.setVisibility(View.VISIBLE);
        
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.GONE);
        }
    }    // Custom formatter for chart axis
    private class TimeAxisValueFormatter extends ValueFormatter {
        private final List<String> labels;
        private final boolean isPerChat;
        
        public TimeAxisValueFormatter(List<String> labels, boolean isPerChat) {
            this.labels = labels;
            this.isPerChat = isPerChat;
        }
        
        @Override
        public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
            int index = (int) value;
            if (index >= 0 && index < labels.size()) {
                return labels.get(index);
            }
            return "";
        }
    }
    
    private void loadRealLineData(List<MoodEntry> moodEntries) {
        List<Entry> entries = new ArrayList<>();
        List<String> xAxisLabels = new ArrayList<>();
        
        if (moodEntries.isEmpty()) {
            return;
        }
        
        boolean isPerChat = !shouldShowDailyView(moodEntries);
        
        if (isPerChat) {
            // Show per chat (chronological) - Take last 10 entries for better visualization
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            
            int startIndex = Math.max(0, moodEntries.size() - 10);
            
            for (int i = startIndex; i < moodEntries.size(); i++) {
                MoodEntry entry = moodEntries.get(i);
                float xValue = i - startIndex; // Sequential index
                entries.add(new Entry(xValue, entry.getMoodScore()));
                
                // Create label with time and short date
                String timeLabel = timeFormat.format(new Date(entry.getTimestamp()));
                String dateLabel = dateFormat.format(new Date(entry.getTimestamp()));
                xAxisLabels.add(timeLabel + "\n" + dateLabel);
            }
        } else {
            // Daily view with multiple entries per day
            loadDailyMoodData(moodEntries, entries, xAxisLabels);
        }
        
        // Setup X-axis formatter
        XAxis xAxis = moodChart.getXAxis();
        xAxis.setValueFormatter(new TimeAxisValueFormatter(xAxisLabels, isPerChat));
        xAxis.setLabelCount(Math.min(xAxisLabels.size(), 7), false);
        
        LineDataSet dataSet = new LineDataSet(entries, isPerChat ? "Per Chat" : "Per Hari");
        dataSet.setColor(Color.parseColor("#7C4DFF"));
        dataSet.setCircleColor(Color.parseColor("#7C4DFF"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(8f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(16f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#7C4DFF"));
        dataSet.setFillAlpha(50);
        
        // Custom value formatter untuk menampilkan emoji
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                return getMoodEmojiFromScore(entry.getY());
            }
        });
        
        LineData lineData = new LineData(dataSet);
        moodChart.setData(lineData);
        moodChart.invalidate();
    }
    
    private boolean shouldShowDailyView(List<MoodEntry> moodEntries) {
        // Show daily view if we have data spanning more than 3 days
        if (moodEntries.size() < 5) return false;
        
        long firstTimestamp = moodEntries.get(0).getTimestamp();
        long lastTimestamp = moodEntries.get(moodEntries.size() - 1).getTimestamp();
        long daysDiff = (lastTimestamp - firstTimestamp) / (24 * 60 * 60 * 1000);
        
        return daysDiff > 3;
    }
    
    private void loadDailyMoodData(List<MoodEntry> moodEntries, List<Entry> entries, List<String> xAxisLabels) {
        // Group by day but keep all entries (not average)
        Map<String, List<Float>> dailyMoods = new HashMap<>();
        
        for (MoodEntry entry : moodEntries) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date(entry.getTimestamp()));
            
            if (!dailyMoods.containsKey(date)) {
                dailyMoods.put(date, new ArrayList<>());
            }
            dailyMoods.get(date).add(entry.getMoodScore());
        }
        
        // Get last 7 days
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        float xIndex = 0;
        
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(calendar.getTime());
            String dateLabel = labelFormat.format(calendar.getTime());
            
            if (dailyMoods.containsKey(date)) {
                List<Float> dayMoods = dailyMoods.get(date);
                
                if (dayMoods.size() == 1) {
                    // Single mood for the day
                    entries.add(new Entry(xIndex, dayMoods.get(0)));
                    xAxisLabels.add(dateLabel);
                    xIndex++;
                } else {
                    // Multiple moods - show each with slight offset
                    for (int j = 0; j < dayMoods.size(); j++) {
                        float offset = j * 0.1f; // Small offset for multiple entries
                        entries.add(new Entry(xIndex + offset, dayMoods.get(j)));
                        if (j == 0) {
                            xAxisLabels.add(dateLabel);
                        }
                    }
                    xIndex++;
                }
            } else {
                // No data for this day - add label but no entry
                xAxisLabels.add(dateLabel);
                xIndex++;
            }
        }
    }
    
    private void loadRealPieData(List<MoodEntry> moodEntries) {
        Map<String, Integer> moodCounts = new HashMap<>();
        
        // Count mood occurrences
        for (MoodEntry entry : moodEntries) {
            String mood = entry.getMoodType();
            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
        }
        
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            String mood = entry.getKey();
            int count = entry.getValue();
            float percentage = (count * 100.0f) / moodEntries.size();
            
            entries.add(new PieEntry(percentage, getMoodEmojiOnly(mood)));
            colors.add(getMoodColor(mood));
        }
        
        if (entries.isEmpty()) {
            entries.add(new PieEntry(100f, "Belum ada data"));
            colors.add(Color.parseColor("#9E9E9E"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f);
        
        PieData pieData = new PieData(dataSet);
        moodDistribution.setData(pieData);
        moodDistribution.invalidate();
    }
    
    private void updateCurrentMoodAndInsight(List<MoodEntry> moodEntries) {
        if (moodEntries.isEmpty()) {
            currentMoodText.setText("😐 Netral");
            moodInsightText.setText("Mulai input mood untuk melihat insight!");
            return;
        }
        
        // Get latest mood (most recent entry)
        MoodEntry latestMood = moodEntries.get(moodEntries.size() - 1);
        String currentMood = latestMood.getMoodType();
        
        // Update current mood display immediately
        currentMoodText.setText(getMoodEmoji(currentMood) + " " + currentMood);
        android.util.Log.d("MoodFragment", "Updated current mood to: " + currentMood);
        
        // Generate insight
        String insight = generateMoodInsight(moodEntries);
        moodInsightText.setText(insight);
    }
    
    private String getMoodEmoji(String mood) {
        switch (mood.toLowerCase()) {
            case "senang":
            case "bahagia":
            case "gembira":
                return "😊";
            case "sedih":
                return "😢";
            case "marah":
            case "kesal":
                return "😠";
            case "cemas":
            case "khawatir":
                return "😟";
            case "excited":
            case "antusias":
                return "😊";
            case "stress":
            case "tertekan":
                return "😫";
            case "calm":
            case "tenang":
                return "🙂";
            case "netral":
            default:
                return "😐";
        }
    }
    
    // Convert mood to emoji-only representation for chart
    private String getMoodEmojiOnly(String mood) {
        return getMoodEmoji(mood);
    }
    
    // Get mood emoji from score for chart display
    private String getMoodEmojiFromScore(float score) {
        if (score >= 9.0f) return "😊"; // Excited
        else if (score >= 8.0f) return "😊"; // Senang  
        else if (score >= 6.5f) return "🙂"; // Calm
        else if (score >= 4.5f) return "😐"; // Netral
        else if (score >= 2.8f) return "😟"; // Cemas
        else if (score >= 2.2f) return "😫"; // Stress
        else if (score >= 1.8f) return "😢"; // Sedih
        else return "😠"; // Marah
    }

    private int getMoodColor(String mood) {
        switch (mood.toLowerCase()) {
            case "senang":
            case "bahagia":
            case "gembira":
                return Color.parseColor("#4CAF50"); // Green - Happy
            case "sedih":
                return Color.parseColor("#2196F3"); // Blue - Sad
            case "marah":
            case "kesal":
                return Color.parseColor("#F44336"); // Red - Angry
            case "cemas":
            case "khawatir":
                return Color.parseColor("#FF9800"); // Orange - Anxious
            case "excited":
            case "antusias":
                return Color.parseColor("#FF6F00"); // Deep Orange - Excited
            case "stress":
            case "tertekan":
                return Color.parseColor("#9C27B0"); // Purple - Stressed
            case "calm":
            case "tenang":
                return Color.parseColor("#00BCD4"); // Cyan - Calm
            case "netral":
            default:
                return Color.parseColor("#9E9E9E"); // Gray - Neutral
        }
    }
    
    private String generateMoodInsight(List<MoodEntry> moodEntries) {
        if (moodEntries.size() < 2) {
            return "Lanjutkan chat dengan AI untuk mendapat insight mood yang lebih akurat!";
        }
        
        // Calculate average mood score
        float totalScore = 0;
        for (MoodEntry entry : moodEntries) {
            totalScore += entry.getMoodScore();
        }
        float avgScore = totalScore / moodEntries.size();
        
        // Get most common mood
        Map<String, Integer> moodCounts = new HashMap<>();
        for (MoodEntry entry : moodEntries) {
            String mood = entry.getMoodType();
            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
        }
        
        String dominantMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantMood = entry.getKey();
            }
        }
        
        // Generate insight based on data - fokus ke mood saat ini
        MoodEntry latestMood = moodEntries.get(moodEntries.size() - 1);
        String currentMood = latestMood.getMoodType();
        String moodEmoji = getMoodEmoji(currentMood);
        
        if (avgScore >= 7.0f) {
            return "✨ Mood Anda cenderung positif! Saat ini: " + moodEmoji + " " + currentMood + ". Pertahankan aktivitas yang membuat Anda bahagia! 😊";
        } else if (avgScore >= 5.0f) {
            return "🌟 Saat ini: " + moodEmoji + " " + currentMood + ". Coba aktivitas yang lebih menyenangkan untuk meningkatkan mood! ✨";
        } else {
            return "💜 Sepertinya Anda perlu lebih banyak dukungan. Saat ini: " + moodEmoji + " " + currentMood + ". Jangan ragu untuk terus bercerita dengan AI! 🤗";
        }
    }
    
    private void setupSingleRecommendation() {
        android.util.Log.d("MoodFragment", "setupSingleRecommendation called");
        
        if (singleRecommendationCard != null) {
            // Setup card visibility but don't set default content
            singleRecommendationCard.setVisibility(View.VISIBLE);
            android.util.Log.d("MoodFragment", "Card visibility set to VISIBLE");
            
            // Don't set default recommendation - wait for mood data based recommendations
            
            singleRecommendationCard.setOnClickListener(v -> {
                // Handle recommendation click
                String title = singleRecommendationTitle != null ? singleRecommendationTitle.getText().toString() : "Me Time Quality";
                String description = singleRecommendationDescription != null ? singleRecommendationDescription.getText().toString() : "Nikmati waktu berkualitas";
                android.util.Log.d("MoodFragment", "Recommendation clicked: " + title);
                android.widget.Toast.makeText(getContext(), 
                    "💡 " + title + " - " + description, 
                    android.widget.Toast.LENGTH_LONG).show();
            });
        } else {
            android.util.Log.e("MoodFragment", "singleRecommendationCard is null in setupSingleRecommendation");
        }
    }
    
    private void updateSingleRecommendation(List<MoodEntry> moodEntries) {
        android.util.Log.d("MoodFragment", "updateSingleRecommendation called with " + moodEntries.size() + " entries");
        
        if (moodEntries.isEmpty()) {
            android.util.Log.d("MoodFragment", "No mood entries, skipping recommendation update");
            return;
        }
        
        // Get most recent mood for recommendations
        MoodEntry latestMood = moodEntries.get(moodEntries.size() - 1);
        String currentMood = latestMood.getMoodType();
        android.util.Log.d("MoodFragment", "Latest mood: " + currentMood);
        
        // Get the best recommendation for current mood
        ActivityRecommendation recommendation = getBestRecommendation(currentMood);
        android.util.Log.d("MoodFragment", "Generated recommendation: " + recommendation.getTitle());
        
        // Check if views are initialized
        if (singleRecommendationIcon == null || singleRecommendationTitle == null || singleRecommendationDescription == null) {
            android.util.Log.e("MoodFragment", "Recommendation views are null! Views not properly initialized.");
            return;
        }
        
        // Update UI
        singleRecommendationIcon.setText(recommendation.getIcon());
        singleRecommendationTitle.setText(recommendation.getTitle());
        singleRecommendationDescription.setText(recommendation.getDescription());
        
        android.util.Log.d("MoodFragment", "Recommendation UI updated successfully");
    }
    
    private ActivityRecommendation getBestRecommendation(String moodType) {
        android.util.Log.d("MoodFragment", "Getting recommendation for mood: " + moodType);
        // Return the best single recommendation based on mood
        switch (moodType.toLowerCase()) {
            case "sedih":
                return new ActivityRecommendation("🎵", "Dengar Musik Favorit", "Putar lagu yang membuat hati senang dan tingkatkan mood", "Hiburan");
                
            case "marah":
            case "kesal":
                return new ActivityRecommendation("🧘", "Meditasi 10 Menit", "Tenangkan pikiran dengan teknik pernapasan dalam", "Relaksasi");
                
            case "cemas":
            case "khawatir":
                return new ActivityRecommendation("🫁", "Latihan Pernapasan", "Gunakan teknik 4-7-8 untuk menenangkan diri", "Relaksasi");
                
            case "senang":
            case "bahagia":
            case "gembira":
                return new ActivityRecommendation("🎨", "Aktivitas Kreatif", "Manfaatkan energi positif untuk berkreasi", "Kreatif");
                
            case "excited":
            case "antusias":
                return new ActivityRecommendation("🏃", "Olahraga Energik", "Salurkan energi tinggi dengan aktivitas fisik yang menyenangkan", "Fisik");
                
            case "stress":
            case "tertekan":
                return new ActivityRecommendation("🌿", "Jalan Santai", "Habiskan waktu di alam atau taman untuk mengurangi tekanan", "Relaksasi");
                
            case "calm":
            case "tenang":
                return new ActivityRecommendation("📚", "Baca Buku Favorit", "Pertahankan ketenangan dengan membaca hal yang menyenangkan", "Edukatif");
                
            default: // Netral
                return new ActivityRecommendation("☕", "Me Time Quality", "Nikmati waktu sendiri dengan minuman hangat favorit", "Relaksasi");
        }
    }
}