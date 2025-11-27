package com.example.atlasevents.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.*;
import com.example.atlasevents.utils.NotificationHelper;
/**
     * Listens for real-time notifications for a specific user and handles their display.
     * Monitors notification preferences, blocked organizers, and incoming notifications, 
     * displaying them via toasts (non-blocking) without altering read status.
 *
 * <p>This class manages three Firestore listeners:
 * <ul>
 *   <li>Preference listener: Monitors user's notification enabled/disabled preference</li>
 *   <li>Blocked organizers listener: Monitors user's list of blocked organizer emails</li>
 *   <li>Notification listener: Monitors incoming notifications when preferences allow</li>
 * </ul>
 * </p>
 *
 * <p>Notifications from blocked organizers are silently marked as read without being displayed.</p>
 *
 * @see Notification
 * @see NotificationHelper
 * @see FirebaseFirestore
 */
public class NotificationListener {

    private static final String TAG = "NotificationListener";
    private final FirebaseFirestore db;
    private ListenerRegistration notifsRegistration;
    private ListenerRegistration prefRegistration;
    private final Activity activity;
    private final SharedPreferences toastPrefs;

    private final String email;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private ListenerRegistration preferencesRegistration;
    private final java.util.Set<String> blockedEmails = new java.util.HashSet<>();
    /**
     * Constructs a new NotificationListener for the specified user and activity.
     *
     * @param activity The Android activity context for displaying notifications
     * @param userEmail The email address of the user to listen for notifications
     * @throws NullPointerException if activity or userEmail is null
     * @see FirebaseFirestore#getInstance()
     */
    public NotificationListener(@NonNull Activity activity, @NonNull String userEmail) {
        this.activity = activity;
        this.db = FirebaseFirestore.getInstance();
        this.email = userEmail;
        this.toastPrefs = activity.getSharedPreferences("notification_toasts", Context.MODE_PRIVATE);
    }

    /**
     * Start listening for unread notifications for the current user.
     * Displays each new notification via NotificationHelper and marks it read.
     * Attaches listeners to monitor user preferences, blocked organizers, and incoming notifications.
     * Notifications from blocked organizers are silently marked as read without being displayed.
     * If user email is null, this method does nothing.
     *
     * @see #stop()
     * @see #attachNotificationsListener()
     * @see #detachNotificationsListener()
     * @see #attachPreferencesListener()
     */
    public void start() {
        if (email == null) return;
        
        // Listen to user's preferences for blocked organizers
        attachPreferencesListener();
        
        // Listen to user doc for notificationsEnabled changes
        DocumentReference userRef = db.collection("users").document(email);
        prefRegistration = userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "pref listener failed", e);
                return;
            }
            if (snapshot == null) return;
            Boolean b = snapshot.getBoolean("notificationsEnabled");
            if (b == null) b = true;
            enabled.set(b);
            if (b) attachNotificationsListener();
            else detachNotificationsListener();
        });
    }
    
    /**
     * Attaches a listener to monitor the user's blocked organizer preferences.
     * Updates the blockedEmails set whenever the preferences change.
     */
    private void attachPreferencesListener() {
        if (email == null) return;
        
        DocumentReference prefRef = db.collection("users")
                .document(email)
                .collection("preferences")
                .document("blockedOrganizers");
        
        preferencesRegistration = prefRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "preferences listener failed", e);
                return;
            }
            
            blockedEmails.clear();
            if (snapshot != null && snapshot.exists()) {
                Object blockedEmailsObj = snapshot.get("blockedEmails");
                if (blockedEmailsObj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) blockedEmailsObj;
                    for (Object item : list) {
                        if (item instanceof String) {
                            blockedEmails.add((String) item);
                        }
                    }
                    Log.d(TAG, "Updated blocked emails: " + blockedEmails.size() + " organizers blocked");
                }
            }
        });
    }
    
    /**
     * Attaches the notifications listener to monitor incoming unread notifications.
     * Only attaches if not already attached and notifications are enabled.
     * Listens for new notifications ordered by creation time (newest first).
     * Automatically filters out notifications from blocked organizers.
     *
     * @throws IllegalStateException if Firestore operations fail
     * @see NotificationHelper#showToast(Activity, String)
     */
    private void attachNotificationsListener() {
        if (notifsRegistration != null) return;
        CollectionReference notifsRef = db.collection("users").document(email).collection("notifications");
        // Listen to all notifications ordered by createdAt (no index needed)
        notifsRegistration = notifsRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "notification snapshot error", e);
                        return;
                    }
                    if (snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            Notification notif = doc.toObject(Notification.class);
                            if (notif == null) {
                                Log.w(TAG, "Failed to parse notification");
                                continue;
                            }
                            
                            // Only process unread notifications
                            Boolean isRead = doc.getBoolean("read");
                            if (isRead != null && isRead) {
                                Log.d(TAG, "Skipping already read notification: " + doc.getId());
                                continue; // Skip already read notifications
                            }
                            
                            // Check if the organizer is blocked
                            String fromOrganizerEmail = notif.getFromOrganizeremail();
                            if (fromOrganizerEmail != null && blockedEmails.contains(fromOrganizerEmail)) {
                                Log.d(TAG, "Skipping notification from blocked organizer: " + fromOrganizerEmail);
                                // Mark as read silently so it doesn't show up again
                                doc.getReference().update("read", true)
                                        .addOnFailureListener(err -> Log.w(TAG, "Unable to mark blocked notif read", err));
                                continue;
                            }
                            
                            // Double-check enabled
                            if (!enabled.get()) {
                                // skip (but not delete)
                                continue;
                            }
                            String notifId = doc.getId();
                            if (hasShownToast(notifId)) {
                                continue;
                            }
                            String title = notif.getTitle() != null ? notif.getTitle() : "Notification";
                            String message = notif.getMessage() != null ? notif.getMessage() : "";
                            NotificationHelper.showToast(activity, title + ": " + message);
                            markToastShown(notifId);
                        }
                    }
                });
    }
    /**
     * Detaches the notifications listener to stop monitoring incoming notifications.
     * Called when notifications are disabled or when stopping the listener.
     *
     * @see #start()
     * @see #stop()
     */
    private void detachNotificationsListener() {
        if (notifsRegistration != null) {
            notifsRegistration.remove();
            notifsRegistration = null;
        }
    }
    /**
     * Stops all listeners and cleans up resources.
     * Removes preference, blocked organizers, and notification listeners.
     * Clears the blocked emails cache.
     * Should be called when the activity is stopped or destroyed.
     *
     * @see #start()
     */
    public void stop() {
        if (prefRegistration != null) {
            prefRegistration.remove();
            prefRegistration = null;
        }
        if (preferencesRegistration != null) {
            preferencesRegistration.remove();
            preferencesRegistration = null;
        }
        detachNotificationsListener();
        blockedEmails.clear();
    }

    private boolean hasShownToast(String notificationId) {
        java.util.Set<String> set = toastPrefs.getStringSet("shown_ids", new java.util.HashSet<>());
        return set != null && set.contains(notificationId);
    }

    private void markToastShown(String notificationId) {
        java.util.Set<String> set = new java.util.HashSet<>(
                toastPrefs.getStringSet("shown_ids", new java.util.HashSet<>()));
        if (set.size() > 500) {
            set.clear();
        }
        set.add(notificationId);
        toastPrefs.edit().putStringSet("shown_ids", set).apply();
    }


}
