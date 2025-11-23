package com.example.moodmate.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.moodmate.models.ChatMessage;
import com.example.moodmate.models.MoodEntry;
import com.example.moodmate.models.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "moodmate.db";
    private static final int DATABASE_VERSION = 3; // Updated version for user-specific data
    
    // Tables
    private static final String TABLE_CHAT = "chat_messages";
    private static final String TABLE_MOOD = "mood_entries";
    private static final String TABLE_USER = "users";
    
    // Chat table columns
    private static final String CHAT_ID = "id";
    private static final String CHAT_MESSAGE = "message";
    private static final String CHAT_IS_USER = "is_user";
    private static final String CHAT_TIMESTAMP = "timestamp";
    private static final String CHAT_USER_ID = "user_id";
    
    // Mood table columns
    private static final String MOOD_ID = "id";
    private static final String MOOD_TYPE = "mood_type";
    private static final String MOOD_SCORE = "mood_score";
    private static final String MOOD_TIMESTAMP = "timestamp";
    private static final String MOOD_SOURCE = "source"; // chat, manual, journal
    private static final String MOOD_USER_ID = "user_id";
    
    // User table columns
    private static final String USER_ID = "id";
    private static final String USER_NAME = "name";
    private static final String USER_EMAIL = "email";
    private static final String USER_PASSWORD = "password";
    private static final String USER_PROFILE_PICTURE = "profile_picture";
    private static final String USER_CREATED_AT = "created_at";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create chat table
        String createChatTable = "CREATE TABLE " + TABLE_CHAT + " (" +
                CHAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CHAT_MESSAGE + " TEXT NOT NULL, " +
                CHAT_IS_USER + " INTEGER NOT NULL, " +
                CHAT_TIMESTAMP + " INTEGER NOT NULL, " +
                CHAT_USER_ID + " INTEGER NOT NULL)";
        db.execSQL(createChatTable);
        
        // Create mood table
        String createMoodTable = "CREATE TABLE " + TABLE_MOOD + " (" +
                MOOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MOOD_TYPE + " TEXT NOT NULL, " +
                MOOD_SCORE + " REAL, " +
                MOOD_SOURCE + " TEXT, " +
                MOOD_TIMESTAMP + " INTEGER NOT NULL, " +
                MOOD_USER_ID + " INTEGER NOT NULL)";
        db.execSQL(createMoodTable);
        
        // Create user table
        String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USER_NAME + " TEXT NOT NULL, " +
                USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                USER_PASSWORD + " TEXT NOT NULL, " +
                USER_PROFILE_PICTURE + " TEXT, " +
                USER_CREATED_AT + " INTEGER NOT NULL)";
        db.execSQL(createUserTable);
        

    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add user table in version 2
            String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                    USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    USER_NAME + " TEXT NOT NULL, " +
                    USER_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    USER_PASSWORD + " TEXT NOT NULL, " +
                    USER_PROFILE_PICTURE + " TEXT, " +
                    USER_CREATED_AT + " INTEGER NOT NULL)";
            db.execSQL(createUserTable);
            android.util.Log.d("DatabaseHelper", "User table created during upgrade");
        }
        
        if (oldVersion < 3) {
            // Add user_id columns to existing tables in version 3
            try {
                db.execSQL("ALTER TABLE " + TABLE_CHAT + " ADD COLUMN " + CHAT_USER_ID + " INTEGER DEFAULT 1");
                db.execSQL("ALTER TABLE " + TABLE_MOOD + " ADD COLUMN " + MOOD_USER_ID + " INTEGER DEFAULT 1");
                android.util.Log.d("DatabaseHelper", "User ID columns added to chat and mood tables");
            } catch (Exception e) {
                android.util.Log.e("DatabaseHelper", "Error adding user_id columns: " + e.getMessage());
                // Fallback: recreate tables
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOOD);
                onCreate(db);
            }
        }
    }
    
    // Chat methods
    public long insertChatMessage(ChatMessage message, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CHAT_MESSAGE, message.getMessage());
        values.put(CHAT_IS_USER, message.isUser() ? 1 : 0);
        values.put(CHAT_TIMESTAMP, message.getTimestamp());
        values.put(CHAT_USER_ID, userId);
        
        long id = db.insert(TABLE_CHAT, null, values);
        android.util.Log.d("DatabaseHelper", "Inserted chat message for userId: " + userId + ", ID: " + id);
        db.close();
        return id;
    }
    
    public List<ChatMessage> getAllChatMessages(int userId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        android.util.Log.d("DatabaseHelper", "Getting chat messages for userId: " + userId);
        String selection = CHAT_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        Cursor cursor = db.query(TABLE_CHAT, null, selection, selectionArgs, null, null, CHAT_TIMESTAMP + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                ChatMessage message = new ChatMessage(
                        cursor.getString(cursor.getColumnIndexOrThrow(CHAT_MESSAGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(CHAT_IS_USER)) == 1,
                        cursor.getLong(cursor.getColumnIndexOrThrow(CHAT_TIMESTAMP))
                );
                messages.add(message);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return messages;
    }
    
    // Mood methods
    public long insertMoodEntry(MoodEntry mood, int userId) {
        SQLiteDatabase db = null;
        long id = -1;
        
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(MOOD_TYPE, mood.getMoodType());
            values.put(MOOD_SCORE, mood.getMoodScore());
            values.put(MOOD_SOURCE, mood.getSource());
            values.put(MOOD_TIMESTAMP, mood.getTimestamp());
            values.put(MOOD_USER_ID, userId);
            
            id = db.insert(TABLE_MOOD, null, values);
            
            // Debug log
            android.util.Log.d("DatabaseHelper", "Inserting mood: " + mood.getMoodType() + 
                    " Score: " + mood.getMoodScore() + " Result ID: " + id);
            
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "Error inserting mood: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return id;
    }
    
    public List<MoodEntry> getMoodEntriesInRange(int userId, long startTime, long endTime) {
        List<MoodEntry> moods = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = MOOD_USER_ID + " = ? AND " + MOOD_TIMESTAMP + " BETWEEN ? AND ?";
        String[] selectionArgs = {String.valueOf(userId), String.valueOf(startTime), String.valueOf(endTime)};
        
        Cursor cursor = db.query(TABLE_MOOD, null, selection, selectionArgs, null, null, MOOD_TIMESTAMP + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                MoodEntry mood = new MoodEntry(
                        cursor.getString(cursor.getColumnIndexOrThrow(MOOD_TYPE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(MOOD_SCORE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(MOOD_SOURCE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(MOOD_TIMESTAMP))
                );
                moods.add(mood);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return moods;
    }
    
    public List<MoodEntry> getAllMoodEntries(int userId) {
        List<MoodEntry> moods = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = this.getReadableDatabase();
            String selection = MOOD_USER_ID + " = ?";
            String[] selectionArgs = {String.valueOf(userId)};
            cursor = db.query(TABLE_MOOD, null, selection, selectionArgs, null, null, MOOD_TIMESTAMP + " ASC");
            
            android.util.Log.d("DatabaseHelper", "Getting mood entries for userId: " + userId);
            android.util.Log.d("DatabaseHelper", "Query executed, cursor count: " + cursor.getCount());
            
            if (cursor.moveToFirst()) {
                do {
                    MoodEntry mood = new MoodEntry(
                            cursor.getString(cursor.getColumnIndexOrThrow(MOOD_TYPE)),
                            cursor.getFloat(cursor.getColumnIndexOrThrow(MOOD_SCORE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MOOD_SOURCE)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(MOOD_TIMESTAMP))
                    );
                    moods.add(mood);
                    android.util.Log.d("DatabaseHelper", "Retrieved mood: " + mood.getMoodType());
                } while (cursor.moveToNext());
            }
            
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "Error getting mood entries: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        
        return moods;
    }
    
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHAT, null, null);
        db.delete(TABLE_MOOD, null, null);
        db.close();
    }
    
    public void clearUserData(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = CHAT_USER_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};
        db.delete(TABLE_CHAT, whereClause, whereArgs);
        
        whereClause = MOOD_USER_ID + " = ?";
        db.delete(TABLE_MOOD, whereClause, whereArgs);
        db.close();
    }
    
    // Debug method to test database
    public void testDatabase() {
        android.util.Log.d("DatabaseHelper", "Testing database connection...");
        
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            android.util.Log.d("DatabaseHelper", "Database opened successfully");
            
            // Test if tables exist
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            android.util.Log.d("DatabaseHelper", "Tables in database:");
            if (cursor.moveToFirst()) {
                do {
                    String tableName = cursor.getString(0);
                    android.util.Log.d("DatabaseHelper", "Table: " + tableName);
                } while (cursor.moveToNext());
            }
            cursor.close();
            
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "Database test error: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    
    // User Management Methods
    
    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            android.util.Log.e("DatabaseHelper", "Error hashing password: " + e.getMessage());
            return password; // Fallback to plain text (not recommended for production)
        }
    }
    
    // Register new user
    public long registerUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(USER_NAME, user.getName());
        values.put(USER_EMAIL, user.getEmail());
        values.put(USER_PASSWORD, hashPassword(user.getPassword()));
        values.put(USER_PROFILE_PICTURE, user.getProfilePicture());
        values.put(USER_CREATED_AT, user.getCreatedAt());
        
        long result = db.insert(TABLE_USER, null, values);
        db.close();
        
        android.util.Log.d("DatabaseHelper", "User registered with ID: " + result);
        return result;
    }
    
    // Login user
    public User loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);
        
        String[] columns = {USER_ID, USER_NAME, USER_EMAIL, USER_PASSWORD, USER_PROFILE_PICTURE, USER_CREATED_AT};
        String selection = USER_EMAIL + " = ? AND " + USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, hashedPassword};
        
        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs, null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User(
                cursor.getInt(cursor.getColumnIndexOrThrow(USER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_PROFILE_PICTURE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(USER_CREATED_AT))
            );
            android.util.Log.d("DatabaseHelper", "User login successful: " + user.getName());
        } else {
            android.util.Log.d("DatabaseHelper", "User login failed for email: " + email);
        }
        
        cursor.close();
        db.close();
        return user;
    }
    
    // Check if email exists
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {USER_ID};
        String selection = USER_EMAIL + " = ?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        
        cursor.close();
        db.close();
        
        android.util.Log.d("DatabaseHelper", "Email exists check for " + email + ": " + exists);
        return exists;
    }
    
    // Get user by email
    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {USER_ID, USER_NAME, USER_EMAIL, USER_PASSWORD, USER_PROFILE_PICTURE, USER_CREATED_AT};
        String selection = USER_EMAIL + " = ?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs, null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User(
                cursor.getInt(cursor.getColumnIndexOrThrow(USER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_PASSWORD)),
                cursor.getString(cursor.getColumnIndexOrThrow(USER_PROFILE_PICTURE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(USER_CREATED_AT))
            );
        }
        
        cursor.close();
        db.close();
        return user;
    }
    
    // Force recreate database (for development/testing)
    public void recreateDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOOD);
        onCreate(db);
        db.close();
        android.util.Log.d("DatabaseHelper", "Database recreated successfully");
    }
}