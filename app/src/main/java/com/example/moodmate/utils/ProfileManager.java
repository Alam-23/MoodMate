package com.example.moodmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.moodmate.database.DatabaseHelper;

import java.security.MessageDigest;

public class ProfileManager {
    private Context context;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;
    
    public ProfileManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("MoodMatePrefs", Context.MODE_PRIVATE);
        this.databaseHelper = new DatabaseHelper(context);
    }
    
    public interface ProfileUpdateCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public void updateUserName(String newName, ProfileUpdateCallback callback) {
        if (newName.trim().isEmpty()) {
            callback.onError("Nama tidak boleh kosong");
            return;
        }
        
        if (newName.trim().length() < 2) {
            callback.onError("Nama minimal 2 karakter");
            return;
        }
        
        String userEmail = sharedPreferences.getString("userEmail", "");
        if (userEmail.isEmpty()) {
            callback.onError("Email pengguna tidak ditemukan");
            return;
        }
        
        boolean success = databaseHelper.updateUserName(userEmail, newName.trim());
        
        if (success) {
            // Update SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userName", newName.trim());
            editor.apply();
            
            callback.onSuccess("✅ Nama berhasil diperbarui");
        } else {
            callback.onError("❌ Gagal memperbarui nama");
        }
    }
    
    public void updatePassword(String currentPassword, String newPassword, String confirmPassword, ProfileUpdateCallback callback) {
        if (currentPassword.trim().isEmpty() || newPassword.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            callback.onError("Semua field harus diisi");
            return;
        }
        
        if (newPassword.length() < 6) {
            callback.onError("Password baru minimal 6 karakter");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            callback.onError("Konfirmasi password tidak cocok");
            return;
        }
        
        String userEmail = sharedPreferences.getString("userEmail", "");
        if (userEmail.isEmpty()) {
            callback.onError("Email pengguna tidak ditemukan");
            return;
        }
        
        // Verify current password
        String hashedCurrentPassword = hashPassword(currentPassword);
        if (!databaseHelper.verifyUserPassword(userEmail, hashedCurrentPassword)) {
            callback.onError("❌ Password lama tidak benar");
            return;
        }
        
        // Update with new password
        String hashedNewPassword = hashPassword(newPassword);
        boolean success = databaseHelper.updateUserPassword(userEmail, hashedNewPassword);
        
        if (success) {
            callback.onSuccess("✅ Password berhasil diperbarui");
        } else {
            callback.onError("❌ Gagal memperbarui password");
        }
    }
    
    public String getCurrentUserName() {
        return sharedPreferences.getString("userName", "");
    }
    
    public String getCurrentUserEmail() {
        return sharedPreferences.getString("userEmail", "");
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }
    
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean verifyCurrentPassword(String email, String hashedPassword) {
        return databaseHelper.verifyUserPassword(email, hashedPassword);
    }
    
    public User getCurrentUser() {
        String name = getCurrentUserName();
        String email = getCurrentUserEmail();
        return new User(name, email);
    }
    
    public static class User {
        public String name;
        public String email;
        
        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}