package com.example.atlasevents.utils;

import android.app.Activity;
import com.example.atlasevents.data.NotificationListener;

/**
 * Helper class to manage notification listeners for activities
 */
public class NotificationManager {
    private static NotificationListener currentListener;
    
    /**
     * Start listening for notifications for a user
     * @param activity The current activity
     * @param userEmail The user's email
     */
    public static void startListening(Activity activity, String userEmail) {
        stopListening(); // Stop any existing listener
        
        if (userEmail != null && !userEmail.isEmpty()) {
            currentListener = new NotificationListener(activity, userEmail);
            currentListener.start();
        }
    }
    
    /**
     * Stop the current notification listener
     */
    public static void stopListening() {
        if (currentListener != null) {
            currentListener.stop();
            currentListener = null;
        }
    }
}