package com.example.moodmate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodmate.database.DatabaseHelper;
import com.example.moodmate.models.User;

public class LoginActivity extends AppCompatActivity {
    
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        setupClickListeners();
        
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("MoodMatePrefs", MODE_PRIVATE);
        
        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMain();
        }
    }
    
    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> navigateToRegister());
    }
    
    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email diperlukan");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password diperlukan");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            return;
        }
        
        // Check credentials
        try {
            User user = databaseHelper.loginUser(email, password);
            if (user != null) {
                // Save login state
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putInt("userId", user.getId());
                editor.putString("userEmail", email);
                editor.putString("userName", user.getName());
                editor.apply();
            
            Toast.makeText(this, "Login berhasil! Selamat datang " + user.getName(), Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else {
                Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Database error: " + e.getMessage());
            Toast.makeText(this, "Error database, coba lagi", Toast.LENGTH_SHORT).show();
            
            // Try to recreate database if users table doesn't exist
            if (e.getMessage() != null && e.getMessage().contains("no such table: users")) {
                try {
                    databaseHelper.recreateDatabase();
                    Toast.makeText(this, "Database diperbaiki, silakan coba lagi", Toast.LENGTH_SHORT).show();
                } catch (Exception recreateError) {
                    android.util.Log.e("LoginActivity", "Failed to recreate database: " + recreateError.getMessage());
                }
            }
        }
    }
    
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
}