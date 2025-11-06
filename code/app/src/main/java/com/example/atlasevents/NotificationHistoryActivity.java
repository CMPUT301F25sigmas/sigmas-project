package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity to display notification history for the current user
 */
public class NotificationHistoryActivity extends AppCompatActivity {
    private static final String TAG = "NotificationHistory";
    
    private LinearLayout notificationsContainer;
    private FirebaseFirestore db;
    private Session session;
    private EventRepository eventRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_history);
        
        // Initialize
        db = FirebaseFirestore.getInstance();
        session = new Session(this);
        eventRepository = new EventRepository();
        
        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Get the container for notification cards
        notificationsContainer = findViewById(R.id.notificationsContainer);
        
        // Load notifications
        loadNotifications();
    }
    
    /**
     * Load all notifications for the current user from Firestore
     */
    private void loadNotifications() {
        String userEmail = session.getUserEmail();
        
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e(TAG, "No user email found in session");
            showEmptyState();
            return;
        }
        
        Log.d(TAG, "Loading notifications for: " + userEmail);
        
        db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationsContainer.removeAllViews(); // Clear existing views
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No notifications found");
                        showEmptyState();
                        return;
                    }
                    
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " notifications");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setNotificationId(document.getId());
                        addNotificationCard(notification);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    showEmptyState();
                });
    }
    
    /**
     * Add a notification card to the UI
     */
    private void addNotificationCard(Notification notification) {
        // Inflate the notification card layout
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.notification_card, notificationsContainer, false);
        
        // Get references to views
        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView recipientsTextView = cardView.findViewById(R.id.notificationRecipientsCount);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        
        // Set notification data
        String groupType = notification.getGroupType() != null ? notification.getGroupType() : "Notification";
        tagTextView.setText(groupType);

        // Set event name
        String eventName = notification.getEventName() != null ? notification.getEventName() : "";
        eventNameTextView.setText(eventName);
        
        // Format timestamp
        if (notification.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(notification.getCreatedAt());
            timestampTextView.setText(formattedDate);
        } else {
            timestampTextView.setText("Just now");
        }
        
        // Set message
        String message = notification.getMessage() != null ? notification.getMessage() : "";
        messageTextView.setText(message);
        
        // For single user notifications, show "1 recipient"
        recipientsTextView.setText("1 recipient");

        // Add click listener to mark as read when clicked
        cardView.setOnClickListener(v -> {
            if (!notification.isRead()) {
                markAsRead(notification.getNotificationId());
                cardView.setAlpha(0.7f);
            }
        });
        
        // Add the card to the container
        notificationsContainer.addView(cardView);
    }
    
    /**
     * Mark a notification as read in Firestore
     */
    private void markAsRead(String notificationId) {
        String userEmail = session.getUserEmail();
        if (userEmail == null || notificationId == null) return;
        
        db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Marked notification as read: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark as read", e));
    }
    
    /**
     * Show empty state when no notifications exist
     */
    private void showEmptyState() {
        notificationsContainer.removeAllViews();
        
        TextView emptyTextView = new TextView(this);
        emptyTextView.setText("No notifications yet");
        emptyTextView.setTextSize(16);
        emptyTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        emptyTextView.setPadding(32, 64, 32, 32);
        emptyTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        notificationsContainer.addView(emptyTextView);
    }
}
