package com.example.moodmate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodmate.database.DatabaseHelper;
import com.example.moodmate.models.User;

public class RegisterActivity extends AppCompatActivity {
    
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        initViews();
        setupClickListeners();
        
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
    }
    
    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> navigateToLogin());
    }
    
    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Nama diperlukan");
            return;
        }
        
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email diperlukan");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password diperlukan");
            return;
        }
        
        if (password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password tidak cocok");
            return;
        }
        
        // Check if email already exists
        if (databaseHelper.isEmailExists(email)) {
            etEmail.setError("Email sudah terdaftar");
            return;
        }
        
        // Register user
        try {
            User user = new User(name, email, password);
            long result = databaseHelper.registerUser(user);
            
            if (result != -1) {
                Toast.makeText(this, "Registrasi berhasil! Silakan login dengan akun baru Anda", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            } else {
                Toast.makeText(this, "Registrasi gagal, coba lagi", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("RegisterActivity", "Database error: " + e.getMessage());
            Toast.makeText(this, "Error database, coba lagi", Toast.LENGTH_SHORT).show();
            
            // Try to recreate database if users table doesn't exist
            if (e.getMessage() != null && e.getMessage().contains("no such table: users")) {
                try {
                    databaseHelper.recreateDatabase();
                    Toast.makeText(this, "Database diperbaiki, silakan coba lagi", Toast.LENGTH_SHORT).show();
                } catch (Exception recreateError) {
                    android.util.Log.e("RegisterActivity", "Failed to recreate database: " + recreateError.getMessage());
                }
            }
        }
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}