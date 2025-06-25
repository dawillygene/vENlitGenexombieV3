package com.dawillygene.venlitgenexombiev2;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    
    private static final String PREF_NAME = "LoginSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "fullName";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Create login session
    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.commit();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get stored session data
    public User getUserDetails() {
        User user = new User();
        user.setUsername(pref.getString(KEY_USERNAME, null));
        user.setEmail(pref.getString(KEY_EMAIL, null));
        user.setFullName(pref.getString(KEY_FULL_NAME, null));
        return user;
    }

    // Clear session data (logout)
    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
