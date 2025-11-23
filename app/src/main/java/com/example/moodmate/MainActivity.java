package com.example.moodmate;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.moodmate.fragments.ChatFragment;
import com.example.moodmate.fragments.MoodFragment;
import com.example.moodmate.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private MoodFragment moodFragment;
    private ChatFragment chatFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupBottomNavigation();
        
        // Load default fragment (Chat)
        if (savedInstanceState == null) {
            chatFragment = new ChatFragment();
            loadFragment(chatFragment);
            showHeader(true); // Show header untuk chat fragment
        }
    }
    
    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }
    
    public void showHeader(boolean show) {
        findViewById(R.id.header_layout).setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    }
    
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                if (chatFragment == null) {
                    chatFragment = new ChatFragment();
                }
                selectedFragment = chatFragment;
            } else if (itemId == R.id.nav_mood) {
                if (moodFragment == null) {
                    moodFragment = new MoodFragment();
                }
                selectedFragment = moodFragment;
                // Refresh mood data when switching to mood tab
                if (moodFragment != null) {
                    moodFragment.refreshMoodData();
                }
            } else if (itemId == R.id.nav_settings) {
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                selectedFragment = settingsFragment;
            }
            
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                // Show header hanya untuk ChatFragment
                showHeader(selectedFragment instanceof ChatFragment);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    // Method to notify mood fragment about new mood data
    public void notifyMoodUpdate() {
        if (moodFragment != null) {
            moodFragment.refreshMoodData();
            android.util.Log.d("MainActivity", "Notified mood fragment about update");
        }
    }
}