package com.example.atlasevents.utils;

import android.app.Activity;
import com.example.atlasevents.data.NotificationListener;

/**
 * Simple helper class to manage real-time notification listeners across different activities.
 * 
 * This class makes sure we only have one active listener at a time, preventing duplicate
 * notifications and memory leaks.
 * when a new activity wants to listen for notifications, this class stops the old listener
 * and starts a new one.
 * 
 * Usage:
 * - Call startListening() in the activity's onStart() or onResume()
 * - Call stopListening() in the activity's onStop() or onPause()
 * 
 * @author CMPUT301F25sigmas
 */
public class NotificationManager {
    /** The currently active notification listener (only one at a time) */
    private static NotificationListener currentListener;
    
    /**
     * Starts listening for real-time notifications for a specific user.
     * If there's already a listener running, it stops that one first before starting a new one.
     * This prevents duplicate notifications and ensures clean transitions between activities.
     * @param activity The activity that will display notification popups (needs context for UI)
     * @param userEmail The email of the user to listen for notifications for
     */
    public static void startListening(Activity activity, String userEmail) {
        // First, stop any existing listener to avoid duplicates
        stopListening();
        
        // Only start a new listener if we have a valid user email
        if (userEmail != null && !userEmail.isEmpty()) {
            currentListener = new NotificationListener(activity, userEmail);
            currentListener.start();
        }
    }
    
    /**
     * Stops the current notification listener if one is active.
     * This is important to call when an activity is closing or going to the background
     * to prevent memory leaks and unnecessary background processing.
     *
     */
    public static void stopListening() {
        if (currentListener != null) {
            currentListener.stop();
            currentListener = null; // Clear the reference to allow garbage collection
        }
    }
}