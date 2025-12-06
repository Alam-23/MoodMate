package com.example.moodmate.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.moodmate.R;
import com.example.moodmate.utils.ProfileManager;

public class EditProfileDialog {
    
    public static void showEditProfile(Context context, ProfileUpdateListener listener) {
        ProfileManager profileManager = new ProfileManager(context);
        
        if (!profileManager.isLoggedIn()) {
            Toast.makeText(context, "This feature is for registered users only", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ProfileManager.User currentUser = profileManager.getCurrentUser();
        
        // Create main layout with app theme colors
        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        layout.setBackgroundColor(Color.parseColor("#3A3A3A"));
        
        // Username field with static white label
        android.widget.TextView usernameLabel = new android.widget.TextView(context);
        usernameLabel.setText("Username");
        usernameLabel.setTextColor(Color.parseColor("#FFFFFF"));
        usernameLabel.setTextSize(16);
        usernameLabel.setPadding(0, 0, 0, 8);
        layout.addView(usernameLabel);
        
        android.widget.EditText editUsername = new android.widget.EditText(context);
        editUsername.setText(currentUser.name);
        editUsername.setTextColor(Color.parseColor("#FFFFFF"));
        editUsername.setBackgroundColor(Color.parseColor("#2D2D2D"));
        editUsername.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams usernameParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        usernameParams.setMargins(0, 0, 0, 20);
        editUsername.setLayoutParams(usernameParams);
        layout.addView(editUsername);
        
        // Email field with static white label
        android.widget.TextView emailLabel = new android.widget.TextView(context);
        emailLabel.setText("Email");
        emailLabel.setTextColor(Color.parseColor("#FFFFFF"));
        emailLabel.setTextSize(16);
        emailLabel.setPadding(0, 0, 0, 8);
        layout.addView(emailLabel);
        
        android.widget.EditText editEmail = new android.widget.EditText(context);
        editEmail.setText(currentUser.email);
        editEmail.setTextColor(Color.parseColor("#B0B0B0"));
        editEmail.setBackgroundColor(Color.parseColor("#1F1F1F"));
        editEmail.setPadding(20, 20, 20, 20);
        editEmail.setEnabled(false);
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emailParams.setMargins(0, 0, 0, 20);
        editEmail.setLayoutParams(emailParams);
        layout.addView(editEmail);
        
        // Current Password field with static white label
        android.widget.TextView currentPasswordLabel = new android.widget.TextView(context);
        currentPasswordLabel.setText("Current Password");
        currentPasswordLabel.setTextColor(Color.parseColor("#FFFFFF"));
        currentPasswordLabel.setTextSize(16);
        currentPasswordLabel.setPadding(0, 0, 0, 8);
        layout.addView(currentPasswordLabel);
        
        android.widget.EditText editCurrentPassword = new android.widget.EditText(context);
        editCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editCurrentPassword.setTextColor(Color.parseColor("#FFFFFF"));
        editCurrentPassword.setBackgroundColor(Color.parseColor("#2D2D2D"));
        editCurrentPassword.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams currentPasswordParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        currentPasswordParams.setMargins(0, 0, 0, 20);
        editCurrentPassword.setLayoutParams(currentPasswordParams);
        layout.addView(editCurrentPassword);
        
        // New Password field with static white label
        android.widget.TextView newPasswordLabel = new android.widget.TextView(context);
        newPasswordLabel.setText("New Password (optional)");
        newPasswordLabel.setTextColor(Color.parseColor("#FFFFFF"));
        newPasswordLabel.setTextSize(16);
        newPasswordLabel.setPadding(0, 0, 0, 8);
        layout.addView(newPasswordLabel);
        
        android.widget.EditText editNewPassword = new android.widget.EditText(context);
        editNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editNewPassword.setTextColor(Color.parseColor("#FFFFFF"));
        editNewPassword.setBackgroundColor(Color.parseColor("#2D2D2D"));
        editNewPassword.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams newPasswordParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        newPasswordParams.setMargins(0, 0, 0, 20);
        editNewPassword.setLayoutParams(newPasswordParams);
        layout.addView(editNewPassword);
        
        scrollView.addView(layout);
        
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setView(scrollView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();
                
        dialog.setOnShowListener(d -> {
            Button updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            updateButton.setOnClickListener(v -> {
                String newUsername = editUsername.getText().toString().trim();
                String currentPassword = editCurrentPassword.getText().toString();
                String newPassword = editNewPassword.getText().toString().trim();
                
                // Validate username
                if (newUsername.isEmpty()) {
                    Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newUsername.length() < 2) {
                    Toast.makeText(context, "Username must be at least 2 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate current password
                if (currentPassword.isEmpty()) {
                    Toast.makeText(context, "Please enter current password", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Validate new password if provided
                if (!newPassword.isEmpty() && newPassword.length() < 6) {
                    Toast.makeText(context, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateProfile(context, profileManager, newUsername, currentPassword, newPassword, dialog, listener);
            });
        });
        
        dialog.show();
    }
    
    private static void updateProfile(Context context, ProfileManager profileManager, 
                                    String newUsername, String currentPassword, String newPassword, 
                                    androidx.appcompat.app.AlertDialog dialog, ProfileUpdateListener listener) {
        
        // First verify current password
        ProfileManager.User currentUser = profileManager.getCurrentUser();
        String hashedCurrentPassword = ProfileManager.hashPassword(currentPassword);
        
        if (!profileManager.verifyCurrentPassword(currentUser.email, hashedCurrentPassword)) {
            Toast.makeText(context, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update username
        profileManager.updateUserName(newUsername, new ProfileManager.ProfileUpdateCallback() {
            @Override
            public void onSuccess(String message) {
                // If new password is provided, update it
                if (!newPassword.isEmpty()) {
                    profileManager.updatePassword(currentPassword, newPassword, newPassword, 
                        new ProfileManager.ProfileUpdateCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onProfileUpdated();
                                dialog.dismiss();
                            }
                            
                            @Override
                            public void onError(String error) {
                                Toast.makeText(context, "Username updated, but password failed: " + error, Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onProfileUpdated();
                                dialog.dismiss();
                            }
                        });
                } else {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onProfileUpdated();
                    dialog.dismiss();
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, "Failed to update profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public interface ProfileUpdateListener {
        void onProfileUpdated();
    }
}