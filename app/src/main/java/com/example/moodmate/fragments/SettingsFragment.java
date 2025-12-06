package com.example.moodmate.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.moodmate.LoginActivity;
import com.example.moodmate.R;
import com.example.moodmate.database.DatabaseHelper;
import com.example.moodmate.dialogs.EditProfileDialog;
import com.example.moodmate.utils.ProfileManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SettingsFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private Button btnLogout, btnEditProfile;
    private CardView userAccountCard;
    private ImageView ivProfilePicture;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper;
    private ProfileManager profileManager;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLaunchers();
    }

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
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        userAccountCard = view.findViewById(R.id.user_account_card);
        ivProfilePicture = view.findViewById(R.id.iv_profile_picture);

        sharedPreferences = getActivity().getSharedPreferences("MoodMatePrefs", getActivity().MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(getContext());
        profileManager = new ProfileManager(getContext());
    }

    private void setupUserInfo() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        boolean hasSkippedLogin = sharedPreferences.getBoolean("hasSkippedLogin", false);

        if (isLoggedIn) {
            // User is logged in
            String userName = sharedPreferences.getString("userName", "Pengguna");
            String userEmail = sharedPreferences.getString("userEmail", "user@moodmate.app");
            
            // Load profile picture from database
            String profileImageBase64 = databaseHelper.getUserProfilePicture(userEmail);

            tvUserName.setText(userName);
            tvUserEmail.setText(userEmail);

            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                loadProfileImage(profileImageBase64);
            }
            btnLogout.setText("Keluar");
            btnEditProfile.setVisibility(View.VISIBLE);
            userAccountCard.setVisibility(View.VISIBLE);

        } else if (hasSkippedLogin) {
            // User skipped login (guest mode)
            String profileImageBase64 = sharedPreferences.getString("profile_image", "");
            
            tvUserName.setText("Guest User");
            tvUserEmail.setText("guest@moodmate.app");
            
            if (!profileImageBase64.isEmpty()) {
                loadProfileImage(profileImageBase64);
            }
            
            btnLogout.setText("Masuk ke Akun");
            btnEditProfile.setVisibility(View.GONE);
            userAccountCard.setVisibility(View.VISIBLE);

        } else {
            // Should not happen, but hide account card
            userAccountCard.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        ivProfilePicture.setOnClickListener(v -> openImagePicker());
        
        btnEditProfile.setOnClickListener(v -> 
            EditProfileDialog.showEditProfile(getContext(), this::setupUserInfo));

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
    }

    private void initializeLaunchers() {
        // Single image picker launcher - system will handle camera/gallery choice
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("SettingsFragment", "Image picker result code: " + result.getResultCode());
                    
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        
                        if (data != null) {
                            Log.d("SettingsFragment", "Intent data received");
                            
                            // Check for camera result first (bitmap in extras)
                            Bundle extras = data.getExtras();
                            if (extras != null) {
                                Log.d("SettingsFragment", "Extras found: " + extras.keySet());
                                
                                if (extras.containsKey("data")) {
                                    Bitmap bitmap = (Bitmap) extras.get("data");
                                    if (bitmap != null) {
                                        Log.d("SettingsFragment", "Camera bitmap received");
                                        setProfileImage(bitmap);
                                        return;
                                    }
                                }
                            }
                            
                            // Handle gallery result (URI in data)
                            Uri imageUri = data.getData();
                            if (imageUri != null) {
                                Log.d("SettingsFragment", "Gallery URI received: " + imageUri);
                                try {
                                    InputStream inputStream = requireActivity().getContentResolver().openInputStream(imageUri);
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    if (bitmap != null) {
                                        Log.d("SettingsFragment", "Gallery bitmap loaded successfully");
                                        setProfileImage(bitmap);
                                    } else {
                                        Log.e("SettingsFragment", "Failed to decode bitmap from URI");
                                        Toast.makeText(getContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(getContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                                    Log.e("SettingsFragment", "Error loading image from URI", e);
                                }
                            } else {
                                Log.w("SettingsFragment", "No URI found in intent data");
                            }
                        } else {
                            Log.w("SettingsFragment", "Intent data is null");
                        }
                    } else {
                        Log.d("SettingsFragment", "Image picker cancelled or failed");
                    }
                }
        );
        
        // Permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d("SettingsFragment", "Camera permission granted, opening picker");
                        openImagePickerWithPermission();
                    } else {
                        Toast.makeText(getContext(), "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openImagePicker() {
        // Log the start of the method
        Log.d("SettingsFragment", "openImagePicker() called");
        
        // Cek permission untuk kamera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SettingsFragment", "Camera permission not granted, requesting permission");
                permissionLauncher.launch(Manifest.permission.CAMERA);
                return;
            }
        }
        
        openImagePickerWithPermission();
    }
    
    private void openImagePickerWithPermission() {
        Log.d("SettingsFragment", "Camera permission already granted or not needed");
        
        // Create gallery intent
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        Log.d("SettingsFragment", "Created gallery intent: " + galleryIntent.toString());
        
        // Create camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d("SettingsFragment", "Created camera intent: " + cameraIntent.toString());
        
        // Create chooser with gallery as primary
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Pilih Foto Profil");
        
        // Add camera as additional option
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            Intent[] intents = {cameraIntent};
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
            Log.d("SettingsFragment", "Camera intent available, added to chooser");
        } else {
            Log.w("SettingsFragment", "No camera app available");
        }
        
        Log.d("SettingsFragment", "Launching chooser intent");
        try {
            imagePickerLauncher.launch(chooserIntent);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error launching image picker: " + e.getMessage());
            Toast.makeText(getContext(), "Error membuka picker foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setProfileImage(Bitmap bitmap) {
        // Scale down bitmap if too large
        Bitmap scaledBitmap = scaleBitmap(bitmap, 200, 200);
        
        // Create circular bitmap
        Bitmap circularBitmap = getCircularBitmap(scaledBitmap);

        // Set to ImageView
        ivProfilePicture.setImageBitmap(circularBitmap);

        // Save to database
        String encodedImage = bitmapToBase64(scaledBitmap);
        String userEmail = sharedPreferences.getString("userEmail", "");
        
        if (!userEmail.isEmpty()) {
            boolean success = databaseHelper.updateUserProfilePicture(userEmail, encodedImage);
            if (success) {
                Toast.makeText(getContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Gagal menyimpan foto profil", Toast.LENGTH_SHORT).show();
            }
        } else {
            // For guest users, still use SharedPreferences as fallback
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("profile_image", encodedImage);
            editor.apply();
            Toast.makeText(getContext(), "Foto profil berhasil diperbarui (guest)", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage(String base64Image) {
        try {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            Bitmap circularBitmap = getCircularBitmap(bitmap);
            ivProfilePicture.setImageBitmap(circularBitmap);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error loading saved profile image", e);
        }
    }

    private Bitmap scaleBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scaleFactor = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scaleFactor);
        int newHeight = Math.round(height * scaleFactor);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        // Create circular path
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        
        // Apply source bitmap with circular mask
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        
        // Calculate matrix to center and scale bitmap
        Matrix matrix = new Matrix();
        float scale = size / (float) Math.max(bitmap.getWidth(), bitmap.getHeight());
        matrix.setScale(scale, scale);
        matrix.postTranslate((size - bitmap.getWidth() * scale) / 2, (size - bitmap.getHeight() * scale) / 2);
        
        canvas.drawBitmap(bitmap, matrix, paint);
        
        return output;
    }
}