package com.example.atlasevents;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {

    private static final String prefs = "AtlasPrefs";
    private static final String emailKey = "loggedInEmail";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public Session(Context context) {
        sharedPreferences = context.getSharedPreferences(prefs, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setUserEmail(String email) {
        editor.putString(Session.emailKey, email);
        editor.apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(emailKey, null);
    }

    public boolean isLoggedIn() {
        return getUserEmail() != null;
    }

    public void logout() {
        editor.remove(emailKey);
        editor.apply();
    }
}
