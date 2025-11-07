package com.example.atlasevents.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.atlasevents.R;
import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class for loading and displaying notification history.
 * 
 * This class handles the logic for:
 * - Loading notifications from Firestore for both entrants and organizers
 * - Creating notification cards for display
 * - Formatting timestamps and data
 * 
 * 
 * @author CMPUT301F25sigmas
 */
public class NotificationHistoryHelper {
    private static final String TAG = "NotificationHistoryHelper";
    
    private final Context context;
    private final FirebaseFirestore db;
    private final LinearLayout notificationsContainer;
    private final NotificationRepository notificationRepository;
    
    /**
     * Interface for callbacks when notifications are loaded or fail to load.
     */
    public interface NotificationLoadCallback {
        void onNotificationsLoaded(int count);
        void onLoadFailed();
    }
    
    /**
     * Interface for handling mark-as-read requests from notification cards.
     */
    public interface MarkAsReadCallback {
        void onMarkAsRead(String notificationId);
    }
    
    /**
     * Creates a new helper instance.
     * 
     * @param context Android context for inflating layouts
     * @param db Firestore instance
     * @param container The LinearLayout where notification cards will be added
     */
    public NotificationHistoryHelper(Context context, FirebaseFirestore db, LinearLayout container) {
        this.context = context;
        this.db = db;
        this.notificationsContainer = container;
        this.notificationRepository = new NotificationRepository();
    }
    
    /**
     * Loads received notifications for an entrant user.
     * 
     * Queries: users/{userEmail}/notifications/
     * Sorted by: createdAt descending (newest first)
     * 
     * @param userEmail The email of the entrant user
     * @param callback Callback for success/failure handling
     * @param markAsReadCallback Callback for mark-as-read actions
     */
    public void loadEntrantReceivedNotifications(String userEmail, NotificationLoadCallback callback, 
                                                   MarkAsReadCallback markAsReadCallback) {
        db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationsContainer.removeAllViews();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No notifications found");
                        callback.onLoadFailed();
                        return;
                    }
                    
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " notifications");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setNotificationId(document.getId());
                        addEntrantNotificationCard(notification, markAsReadCallback);
                    }
                    
                    callback.onNotificationsLoaded(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    callback.onLoadFailed();
                });
    }
    
    /**
     * Loads sent notifications for an organizer user from notification_logs.
     * 
     * Queries: notification_logs collection
     * Filtered by: fromOrganizer field matching userEmail
     * Sorted by: createdAt descending (newest first)
     * 
     * @param userEmail The email of the organizer user
     * @param callback Callback for success/failure handling
     */
    public void loadOrganizerSentNotifications(String userEmail, NotificationLoadCallback callback) {
        db.collection("notification_logs")
                .whereEqualTo("fromOrganizer", userEmail)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationsContainer.removeAllViews();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No sent notifications found");
                        callback.onLoadFailed();
                        return;
                    }
                    
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " sent notifications");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> logData = document.getData();
                        addOrganizerNotificationCard(logData);
                    }
                    
                    callback.onNotificationsLoaded(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sent notifications", e);
                    callback.onLoadFailed();
                });
    }
    
    /**
     * Loads ALL notification logs for admin review from notification_logs collection.
     * Uses NotificationRepository's existing getNotificationLogs method.
     * 
     * @param callback Callback for success/failure handling
     */
    public void loadAdminAllNotificationLogs(NotificationLoadCallback callback) {
        notificationRepository.getNotificationLogs(new NotificationRepository.NotificationLogsCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> logs) {
                notificationsContainer.removeAllViews();
                
                if (logs.isEmpty()) {
                    Log.d(TAG, "No notification logs found");
                    callback.onLoadFailed();
                    return;
                }
                
                Log.d(TAG, "Found " + logs.size() + " notification logs");
                
                for (Map<String, Object> logData : logs) {
                    addAdminNotificationCard(logData);
                }
                
                callback.onNotificationsLoaded(logs.size());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading admin notification logs", e);
                callback.onLoadFailed();
            }
        });
    }

    /**
     * Creates and adds a notification card for entrants
     */
    private void addEntrantNotificationCard(Notification notification, MarkAsReadCallback markAsReadCallback) {
        addNotificationCard(
            notification.getGroupType(),
            notification.getEventName(),
            formatTimestamp(notification.getCreatedAt()),
            notification.getMessage(),
            "1 recipient",
            markAsReadCallback
        );
    }
    
    /**
     * Creates and adds a notification card for organizers.
     */
    private void addOrganizerNotificationCard(Map<String, Object> logData) {
        addNotificationCard(
            getString(logData, "groupType", "Notification"),
            getString(logData, "eventName", "N/A"),
            formatFirestoreTimestamp(logData.get("createdAt")),
            getString(logData, "message", ""),
            "X recipient", //TODO update with actual count of recipients
            null
        );
    }
    
    /**
     * Creates and adds a notification card for admin view.
     */
    private void addAdminNotificationCard(Map<String, Object> logData) {
        String fromOrganizer = getString(logData, "fromOrganizer", "Unknown");
        //String recipient = getString(logData, "recipient", "Unknown");
        String status = getString(logData, "status", "UNKNOWN");
        
        addNotificationCard(
            getString(logData, "groupType", "Notification"),
            getString(logData, "eventName", "N/A"),
            formatFirestoreTimestamp(logData.get("createdAt")),
            getString(logData, "message", ""),
            "From: " + fromOrganizer, // " → " + recipient + " (" + status + ")"
            null
        );
    }
    
    /**
     * Helper to safely get string from map.
     */
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Common method to create and add a notification card.
     */
    private void addNotificationCard(String groupType, String eventName, String timestamp,
                                      String message, String recipientInfo, MarkAsReadCallback markAsReadCallback) {
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.notification_card, notificationsContainer, false);
        
        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView recipientsTextView = cardView.findViewById(R.id.notificationRecipientsCount);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        
        tagTextView.setText(groupType != null ? groupType : "Notification");
        eventNameTextView.setText(eventName != null ? eventName : "");
        timestampTextView.setText(timestamp);
        messageTextView.setText(message != null ? message : "");
        recipientsTextView.setText(recipientInfo);
        
        notificationsContainer.addView(cardView);
    }

    /**
     * Formats a Date object into a readable timestamp.
     * 
     * @param date The date to format
     * @return Formatted string like "2025-11-05 14:30" or "Just now"
     */
    private String formatTimestamp(Date date) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
        return "Just now";
    }
    
    /**
     * Formats a Firestore Timestamp object into a readable timestamp.
     * 
     * @param timestampObj The Firestore Timestamp object
     * @return Formatted string like "2025-11-05 14:30" or "Just now"
     */
    private String formatFirestoreTimestamp(Object timestampObj) {
        if (timestampObj != null) {
            if (timestampObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) timestampObj;
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return sdf.format(date);
            } else if (timestampObj instanceof Date) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return sdf.format((Date) timestampObj);
            }
        }
        return "Just now";
    }
}
