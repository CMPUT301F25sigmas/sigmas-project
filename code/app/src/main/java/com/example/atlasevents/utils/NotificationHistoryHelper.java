package com.example.atlasevents.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.example.atlasevents.LotteryService;
import com.example.atlasevents.NotificationHistoryActivity;
import com.example.atlasevents.R;
import com.example.atlasevents.Session;
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
import java.util.concurrent.TimeUnit;

/**
 * Helper class for loading and displaying notification history.
 *
 * This class handles the logic for:
 * - Loading notifications from Firestore for both entrants and organizers
 * - Creating notification cards for display
 * - Formatting timestamps and data
 * - sending out lottery selection invites and handling responses/
 *
 *
 * @author CMPUT301F25sigmas
 * @version 2.0
 * @see NotificationHistoryActivity
 * @see NotificationRepository
 * @see LotteryService
 */
public class NotificationHistoryHelper {
    private static final String TAG = "NotificationHistoryHelper";

    private final Context context;
    private final FirebaseFirestore db;
    private final LinearLayout notificationsContainer;
    private final NotificationRepository notificationRepository;
    private String currentUserEmail;
    private Button acceptButton, declineButton;
    private TextView responseDeadline;

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
    public void loadEntrantReceivedNotifications(String userEmail, NotificationLoadCallback callback, MarkAsReadCallback markAsReadCallback) {
        fetchBlockedOrganizers(userEmail, blockedEmails -> db.collection("users")
                .document(userEmail)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .whereNotEqualTo("type", "EventInvitation") // EXCLUDE event invitations
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No notifications found");
                        callback.onLoadFailed();
                        return;
                    }

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " notifications");
                    int displayed = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notification.setNotificationId(document.getId());
                        String organizerEmail = notification.getFromOrganizeremail();
                        if (organizerEmail != null && blockedEmails.contains(organizerEmail)) {
                            continue;
                        }
                        addEntrantNotificationCard(notification, markAsReadCallback);
                        displayed++;
                    }

                    if (displayed == 0) {
                        callback.onLoadFailed();
                    } else {
                        callback.onNotificationsLoaded(displayed);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    callback.onLoadFailed();
                }));
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
                .document(userEmail)
                .collection("logs")
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
        // Check if this is an invitation notification
        boolean isInvitation = "Invitation".equals(notification.getType()) ||
                "Invitation".equals(notification.getGroupType()) ||
                (notification.getTitle() != null && notification.getTitle().contains("Invitation")) ||
                (notification.getMessage() != null && notification.getMessage().contains("selected from the waitlist"));

        if (isInvitation) {
            addInvitationNotificationCard(notification, markAsReadCallback);
        } else {
            addRegularNotificationCard(
                    notification.getGroupType(),
                    notification.getEventName(),
                    formatTimestamp(notification.getCreatedAt()),
                    notification.getMessage(),
                    notification.getRecipientCount() + (notification.getRecipientCount() == 1 ? " recipient" : " recipients"),
                    markAsReadCallback,
                    notification
            );
        }
    }

    /**
     * Creates and adds a special notification card for event invitations with Accept/Decline buttons
     */
    private void addInvitationNotificationCard(Notification notification, MarkAsReadCallback markAsReadCallback) {
        debugInvitationFlow(notification);
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.notification_invite, notificationsContainer, false);

        // Initialize views
        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView titleTextView = cardView.findViewById(R.id.notificationTitle);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView responseDeadline = cardView.findViewById(R.id.responseDeadline);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        TextView organizerTextView = cardView.findViewById(R.id.notificationOrganizer);
        Button acceptButton = cardView.findViewById(R.id.acceptButton);
        Button declineButton = cardView.findViewById(R.id.declineButton);

        // Set notification data
        tagTextView.setText("Event Invitation");
        titleTextView.setText(notification.getTitle() != null ? notification.getTitle() : "You're Invited!");
        messageTextView.setText(notification.getMessage() != null ? notification.getMessage() : "");
        eventNameTextView.setText(notification.getEventName() != null ? notification.getEventName() : "");
        organizerTextView.setText("Organized by: " + (notification.getFromOrganizeremail() != null ?
                notification.getFromOrganizeremail() : "Unknown"));
        timestampTextView.setText(formatTimestamp(notification.getCreatedAt()));

        // Set up response deadline
        if (notification.getExpirationTime() > 0) {
            long timeRemaining = notification.getExpirationTime() - System.currentTimeMillis();
            if (timeRemaining > 0) {
                String timeText = "⏰ Respond within " + formatTimeRemaining(timeRemaining);
                responseDeadline.setText(timeText);
            } else {
                responseDeadline.setText("⏰ Response time expired");
                acceptButton.setEnabled(false);
                declineButton.setEnabled(false);
            }
        } else {
            // Default 24-hour expiration if not set
            responseDeadline.setText("⏰ Respond within 24 hours");
        }

        // Set up button click listeners
        LotteryService lotteryService = new LotteryService();

        acceptButton.setOnClickListener(v -> {
            handleInvitationResponse(notification, true, lotteryService, acceptButton, declineButton, responseDeadline);
            // Mark as read when user responds
            if (markAsReadCallback != null && notification.getNotificationId() != null) {
                markAsReadCallback.onMarkAsRead(notification.getNotificationId());
            }
        });

        declineButton.setOnClickListener(v -> {
            handleInvitationResponse(notification, false, lotteryService, acceptButton, declineButton, responseDeadline);
            // Mark as read when user responds
            if (markAsReadCallback != null && notification.getNotificationId() != null) {
                markAsReadCallback.onMarkAsRead(notification.getNotificationId());
            }
        });

        // Check if user has already responded to this invitation
        checkInvitationStatus(notification, acceptButton, declineButton, responseDeadline);

        notificationsContainer.addView(cardView);
    }

    /**
            * Handles the invitation response when user clicks Accept/Decline
 */
    private void handleInvitationResponse(Notification notification, boolean accepted,
                                          LotteryService lotteryService, Button acceptButton,
                                          Button declineButton, TextView responseDeadline) {
        // Disable buttons immediately to prevent multiple clicks
        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);

        // Show processing state
        responseDeadline.setText("Processing...");

        // Get current user email from session
        String userEmail = getCurrentUserEmail();

        if (userEmail == null || notification.getEventId() == null) {
            responseDeadline.setText("Error: Unable to process");
            return;
        }

        lotteryService.handleInvitationResponse(notification.getEventId(), userEmail, accepted,
                new LotteryService.InvitationResponseCallback() {
                    @Override
                    public void onResponseSuccess(boolean accepted) {
                        // Update UI on main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            String status = accepted ? "✓ Invitation Accepted" : "✗ Invitation Declined";
                            responseDeadline.setText(status);

                            // Change text color based on response
                            int color = accepted ?
                                    ContextCompat.getColor(context, android.R.color.holo_green_dark) :
                                    ContextCompat.getColor(context, android.R.color.holo_red_dark);
                            responseDeadline.setTextColor(color);
                        });
                    }

                    @Override
                    public void onResponseFailed(Exception exception) {
                        // Re-enable buttons on error
                        new Handler(Looper.getMainLooper()).post(() -> {
                            acceptButton.setEnabled(true);
                            declineButton.setEnabled(true);
                            responseDeadline.setText("Error - please try again");
                            responseDeadline.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                        });
                    }
                });
    }

    /**
     * Checks if user has already responded to this invitation and updates UI accordingly
     */
    private void checkInvitationStatus(Notification notification, Button acceptButton,
                                       Button declineButton, TextView responseDeadline) {
        // You might want to check Firestore to see if this user is already in accepted/declined list
        // For now, we'll just check if the notification has been marked as responded
        if (notification.isResponded()) {
            acceptButton.setEnabled(false);
            declineButton.setEnabled(false);
            responseDeadline.setText(notification.isAccepted() ? "✓ Already Accepted" : "✗ Already Declined");
        }
    }

    /**
     * Gets the current logged-in user's email
     */
    /**
     * Sets the current user email from the activity
     */
    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        Log.d(TAG, "User email set in helper: " + email);
    }

    /**
     * Gets the current logged-in user's email
     */
    private String getCurrentUserEmail() {
        if (currentUserEmail != null) {
            Log.d(TAG, "Using email passed from activity: " + currentUserEmail);
            return currentUserEmail;
        }

        // Fallback to Session
        Log.d(TAG, "Falling back to Session for email");
        try {
            Session session = new Session(context);
            String email = session.getUserEmail();
            Log.d(TAG, "Retrieved from Session: " + email);
            return email;
        } catch (Exception e) {
            Log.e(TAG, "Error getting user email from Session", e);
            return null;
        }
    }


    /**
     * Formats time remaining into human-readable format
     */
    private String formatTimeRemaining(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " minutes";
        }
    }

    /**
     * Common method to create regular notification cards (non-invitation)
     */
    private void addRegularNotificationCard(String groupType, String eventName, String timestamp,
                                            String message, String recipientInfo,
                                            MarkAsReadCallback markAsReadCallback, Notification notification) {
        CardView cardView = (CardView) LayoutInflater.from(context)
                .inflate(R.layout.notification_card, notificationsContainer, false);

        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView recipientsTextView = cardView.findViewById(R.id.notificationRecipientsCount);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        View root = cardView.findViewById(R.id.notification_root);

        tagTextView.setText(groupType != null ? groupType : "Notification");
        eventNameTextView.setText(eventName != null ? eventName : "");
        timestampTextView.setText(timestamp);
        messageTextView.setText(message != null ? message : "");
        recipientsTextView.setText(recipientInfo);

        // Tint for unread vs read
        Boolean isRead = notification.isRead();
        if (isRead == null || isRead) {
            root.setBackgroundColor(context.getColor(android.R.color.white));
        } else {
            root.setBackgroundColor(context.getColor(R.color.theme));
        }

        // Set click listener to mark as read and update tint
        if (markAsReadCallback != null && notification.getNotificationId() != null) {
            cardView.setOnClickListener(v -> {
                markAsReadCallback.onMarkAsRead(notification.getNotificationId());
                root.setBackgroundColor(context.getColor(android.R.color.white));
            });
        }

        notificationsContainer.addView(cardView);
    }


    /**
     * Creates and adds a notification card for organizers.
     */
    private void addOrganizerNotificationCard(Map<String, Object> logData) {
        Object countObj = logData.get("recipientCount");
        String recipientInfo = "1 recipient"; // Default
        if (countObj instanceof Number) {
            int count = ((Number) countObj).intValue();
            recipientInfo = count + (count == 1 ? " recipient" : " recipients");
        }
        addNotificationCard(
            getString(logData, "groupType", "Notification"),
            getString(logData, "eventName", "N/A"),
            formatFirestoreTimestamp(logData.get("createdAt")),
            getString(logData, "message", ""),
            recipientInfo,
            null
        );
    }

    /**
     * Creates and adds a notification card for admin view.
     */
    private void addAdminNotificationCard(Map<String, Object> logData) {
        String fromOrganizer = getString(logData, "fromOrganizer", "Unknown");
        Object countObj = logData.get("recipientCount");
        String recipientInfo = "1 recipient"; // Default
        if (countObj instanceof Number) {
            int count = ((Number) countObj).intValue();
            recipientInfo = count + (count == 1 ? " recipient" : " recipients");
        }
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
    private void addNotificationCard(String groupType, String eventName, String timestamp, String message, String recipientInfo, MarkAsReadCallback markAsReadCallback) {
        View cardView = LayoutInflater.from(context)
                .inflate(R.layout.notification_card, notificationsContainer, false);

        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView timestampTextView = cardView.findViewById(R.id.notificationTimestamp);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        TextView recipientsTextView = cardView.findViewById(R.id.notificationRecipientsCount);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        View root = cardView.findViewById(R.id.notification_root);

        tagTextView.setText(groupType != null ? groupType : "Notification");
        eventNameTextView.setText(eventName != null ? eventName : "");
        timestampTextView.setText(timestamp);
        messageTextView.setText(message != null ? message : "");
        recipientsTextView.setText(recipientInfo);
        root.setBackgroundColor(context.getColor(android.R.color.white));


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

    /**
     * Temporary debug method to test invitation response
     */
    private void debugInvitationFlow(Notification notification) {
        Log.d(TAG, "=== DEBUG INVITATION FLOW ===");

        // Test current user email
        String userEmail = getCurrentUserEmail();
        Log.d(TAG, "Current User Email: " + userEmail);

        // Test SharedPreferences access
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        Log.d(TAG, "SharedPreferences contains userEmail: " + prefs.contains("userEmail"));
        Log.d(TAG, "SharedPreferences userEmail value: " + prefs.getString("userEmail", "NOT_FOUND"));

        // Log all SharedPreferences keys
        Map<String, ?> allEntries = prefs.getAll();
        Log.d(TAG, "All SharedPreferences entries:");
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d(TAG, "  " + entry.getKey() + ": " + entry.getValue());
        }

        // Test notification data
        if (notification != null) {
            Log.d(TAG, "Notification Data:");
            Log.d(TAG, "  - Event ID: " + notification.getEventId());
            Log.d(TAG, "  - Type: " + notification.getType());
            Log.d(TAG, "  - Title: " + notification.getTitle());
            Log.d(TAG, "  - Message: " + notification.getMessage());
            Log.d(TAG, "  - Organizer: " + notification.getFromOrganizeremail());
            Log.d(TAG, "  - Notification ID: " + notification.getNotificationId());
        } else {
            Log.d(TAG, "Notification is NULL!");
        }

        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * Fetches the list of blocked organizer emails for the user.
     */
    private void fetchBlockedOrganizers(String userEmail, java.util.function.Consumer<List<String>> callback) {
        db.collection("users")
                .document(userEmail)
                .collection("preferences")
                .document("blockedOrganizers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> blocked = new java.util.ArrayList<>();
                    if (snapshot.exists()) {
                        List<String> stored = (List<String>) snapshot.get("blockedEmails");
                        if (stored != null) {
                            blocked.addAll(stored);
                        }
                    }
                    callback.accept(blocked);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to load blocked organizers, defaulting to none", e);
                    callback.accept(new java.util.ArrayList<>());
                });
    }
}
