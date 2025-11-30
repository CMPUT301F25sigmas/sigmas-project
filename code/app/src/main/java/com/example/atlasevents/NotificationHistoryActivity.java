package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.atlasevents.Session;
import com.example.atlasevents.LotteryService;
import com.example.atlasevents.data.UserRepository;
import com.example.atlasevents.data.model.Notification;
import com.example.atlasevents.utils.NotificationHistoryHelper;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Notification History - Shows notifications based on user type.
 * 
 * For ENTRANTS: Displays all notifications they have received.
 * For ORGANIZERS: Displays all notifications they have sent (from notification_logs).
 * 
 * This activity coordinates the UI and delegates the data loading and card creation
 * to NotificationHistoryHelper for better code organization.
 * @see LotteryService for handlenotification
 * @author CMPUT301F25sigmas
 * @version 2.0
 */
public class NotificationHistoryActivity extends AppCompatActivity {
    private static final String TAG = "NotificationHistory";
    
    private LinearLayout notificationsContainer;
    private FirebaseFirestore db;
    private Session session;
    private UserRepository userRepository;
    private NotificationHistoryHelper notificationHelper;
    private boolean showOrganizerSent;

    /**
     * Called when the activity is created.
     * Sets up the UI, initializes helper, and loads notifications.
     * 
     * @param savedInstanceState Bundle containing saved state (unused here)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_history);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize Firebase and session
        db = FirebaseFirestore.getInstance();
        session = new Session(this);
        userRepository = new UserRepository();
        
        // Get the container where we'll add notification cards
        notificationsContainer = findViewById(R.id.notificationsContainer);
        
        // Initialize the helper
        notificationHelper = new NotificationHistoryHelper(this, db, notificationsContainer);
        showOrganizerSent = getIntent().getBooleanExtra("organizerHistory", false);
        
        // Setup back button to return to previous screen
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Load and display all notifications for this user
        loadNotifications();
    }
    
    /**
     * Loads notifications based on user type.
     * Delegates to helper for actual loading and display.
     */
    private void loadNotifications() {
        String userEmail = session.getUserEmail();
        
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e(TAG, "No user email found in session");
            showEmptyState();
            return;
        }
        
        Log.d(TAG, "Loading notifications for: " + userEmail);
        
        // Check user type to determine which notifications to load
        userRepository.getUser(userEmail, user -> {
            if (user == null) {
                Log.e(TAG, "User not found");
                showEmptyState();
                return;
            }
            
            String userType = user.getUserType();
            Log.d(TAG, "User type: " + userType);
            
            NotificationHistoryHelper.NotificationLoadCallback callback = 
                new NotificationHistoryHelper.NotificationLoadCallback() {
                    @Override
                    public void onNotificationsLoaded(int count) {
                        Log.d(TAG, "Successfully loaded " + count + " notifications");
                    }

                    @Override
                    public void onLoadFailed() {
                        showEmptyState();
                    }
                };

            // PASSING THE EMAIL TO HELPER BEFORE LOADING
            notificationHelper.setCurrentUserEmail(userEmail);
            
            if ("Admin".equals(userType)) {
                // Admin sees ALL notification logs from the system
                notificationHelper.loadAdminAllNotificationLogs(callback);
            } else if ("Organizer".equals(userType) || showOrganizerSent) {
                // Organizer sees only notifications they sent
                notificationHelper.loadOrganizerSentNotifications(userEmail, callback);
            } else {
                // Entrant sees notifications they received
                notificationHelper.loadEntrantReceivedNotifications(
                    userEmail, 
                    callback,
                    this::markAsRead
                );
            }
        });
    }
    
    /**
     * Marks a notification as read in Firestore.
     * Called when an entrant taps a notification card.
     * 
     * @param notificationId The ID of the notification to mark as read
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
     * Shows empty state when no notifications are available.
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
