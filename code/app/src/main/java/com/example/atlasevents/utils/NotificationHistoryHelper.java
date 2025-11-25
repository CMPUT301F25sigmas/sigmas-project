package com.example.atlasevents.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.atlasevents.R;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
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
     * Queries: notification_logs/{userEmail}/event
     * Filtered by: fromOrganizer field matching userEmail
     * Sorted by: createdAt descending (newest first)
     * 
     * @param userEmail The email of the organizer user
     * @param callback Callback for success/failure handling
     */
    public void loadOrganizerSentNotifications(String userEmail, NotificationLoadCallback callback) {
        db.collection("notification_logs")
                .document(userEmail)
                .collection("AllNotfications")
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
     * Simple data holder for notification card information.
     */
    private static class NotificationCardData {
        final String groupType;
        final String eventName;
        final String timestamp;
        final String message;
        final String recipientInfo;
        final boolean isRead;
        final String notificationId;

        NotificationCardData(String groupType, String eventName, String timestamp,
                             String message, String recipientInfo, boolean isRead, String notificationId) {
            this.groupType = groupType;
            this.eventName = eventName;
            this.timestamp = timestamp;
            this.message = message;
            this.recipientInfo = recipientInfo;
            this.isRead = isRead;
            this.notificationId = notificationId;
        }
    }


    /**
     * Creates and adds a notification card for entrants.
     * 
     * @param notification The notification to display
     * @param markAsReadCallback Callback for when the card is clicked
     */
    private void addEntrantNotificationCard(Notification notification, MarkAsReadCallback markAsReadCallback) {
        NotificationCardData data = new NotificationCardData(
            notification.getGroupType(),
            notification.getEventName(),
            formatTimestamp(notification.getCreatedAt()),
            notification.getMessage(),
            "1 recipient",
            notification.isRead(),
            notification.getNotificationId()
        );
        
        addNotificationCard(data, markAsReadCallback);
    }
    
    /**
     * Creates and adds a notification card for organizers.
     * 
     * @param logData The notification log data from Firestore
     */
    private void addOrganizerNotificationCard(Map<String, Object> logData) {
        String recipient = logData.get("recipient") != null ? logData.get("recipient").toString() : "Unknown";
        String status = logData.get("status") != null ? logData.get("status").toString() : "UNKNOWN";
        String recipientInfo = "X recipient"; //TODO

        
        NotificationCardData data = new NotificationCardData(
            logData.get("groupType") != null ? logData.get("groupType").toString() : "Notification",
            logData.get("eventName") != null ? logData.get("eventName").toString() : "N/A",
            formatFirestoreTimestamp(logData.get("createdAt")),
            logData.get("message") != null ? logData.get("message").toString() : "",
            recipientInfo,
            true, // Organizer logs are always "read" (no interaction needed)
            null  // No notification ID for logs
        );
        
        addNotificationCard(data, null);
    }
    
    /**
     * Common method to create and add a notification card with given data.
     * 
     * @param data The notification data to display
     * @param markAsReadCallback Optional callback for mark-as-read (null for organizer cards)
     */
    private void addNotificationCard(NotificationCardData data, MarkAsReadCallback markAsReadCallback) {
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.notification_card, notificationsContainer, false);
        
        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView recipientsTextView = cardView.findViewById(R.id.notificationRecipientsCount);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        
        tagTextView.setText(data.groupType != null ? data.groupType : "Notification");
        eventNameTextView.setText(data.eventName != null ? data.eventName : "");
        timestampTextView.setText(data.timestamp);
        messageTextView.setText(data.message != null ? data.message : "");
        recipientsTextView.setText(data.recipientInfo);
    
        
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