package com.example.atlasevents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class Session {

    private static final String prefs = "AtlasPrefs";
    private static final String emailKey = "loggedInEmail";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor sharedPreferencesEditor;

    public Session(Context context) {
        sharedPreferences = context.getSharedPreferences(prefs, Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public void setUserEmail(String email) {
        sharedPreferencesEditor.putString(Session.emailKey, email);
        sharedPreferencesEditor.apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(emailKey, null);
    }

    public boolean isLoggedIn() {
        return getUserEmail() != null;
    }

    public void logout() {
        sharedPreferencesEditor.remove(emailKey);
        sharedPreferencesEditor.apply();
    }

    public void logoutAndRedirect(Activity activity) {
        logout();
        Intent intent = new Intent(activity, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
