package com.example.moodmate.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.moodmate.LoginActivity;
import com.example.moodmate.R;
import com.example.moodmate.database.DatabaseHelper;

public class SettingsFragment extends Fragment {
    
    private TextView tvUserName, tvUserEmail;
    private Button btnLogout;
    private CardView userAccountCard;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        initViews(view);
        setupUserInfo();
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        userAccountCard = view.findViewById(R.id.user_account_card);
        
        sharedPreferences = getActivity().getSharedPreferences("MoodMatePrefs", getActivity().MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(getContext());
    }
    
    private void setupUserInfo() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        boolean hasSkippedLogin = sharedPreferences.getBoolean("hasSkippedLogin", false);
        
        if (isLoggedIn) {
            // User is logged in
            String userName = sharedPreferences.getString("userName", "Pengguna");
            String userEmail = sharedPreferences.getString("userEmail", "user@moodmate.app");
            
            tvUserName.setText(userName);
            tvUserEmail.setText(userEmail);
            btnLogout.setText("Keluar");
            userAccountCard.setVisibility(View.VISIBLE);
            
        } else if (hasSkippedLogin) {
            // User skipped login (guest mode)
            tvUserName.setText("Guest User");
            tvUserEmail.setText("guest@moodmate.app");
            btnLogout.setText("Masuk ke Akun");
            userAccountCard.setVisibility(View.VISIBLE);
            
        } else {
            // Should not happen, but hide account card
            userAccountCard.setVisibility(View.GONE);
        }
    }
    
    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> {
            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
            
            if (isLoggedIn) {
                showLogoutConfirmation();
            } else {
                // Guest user - navigate to login
                navigateToLogin();
            }
        });
    }
    
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari akun?")
                .setPositiveButton("Keluar", (dialog, which) -> performLogout())
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    private void performLogout() {
        // Clear user session
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        // Clear database (optional - you might want to keep mood data)
        // databaseHelper.clearAllData();
        
        Toast.makeText(getContext(), "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show();
        
        // Navigate to login
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }}