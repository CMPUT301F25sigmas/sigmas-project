package com.example.atlasevents;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.Event;
import com.example.atlasevents.LotteryService;
import com.example.atlasevents.R;
import com.example.atlasevents.Session;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity for entrants to view and manage their event invitations.
 * <p>
 * This activity displays all pending event invitations with Accept/Decline buttons.
 * Invitations are separate from regular notifications and bypass opt-out settings.
 * </p>
 */
public class EventInvitesActivity extends AppCompatActivity {
    private static final String TAG = "EventInvitesActivity";

    private LinearLayout invitesContainer;
    private FirebaseFirestore db;
    private Session session;
    private LotteryService lotteryService;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_landing);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize
        db = FirebaseFirestore.getInstance();
        session = new Session(this);
        lotteryService = new LotteryService();

        // Setup views
        invitesContainer = findViewById(R.id.invitesContainer);
        emptyStateText = findViewById(R.id.emptyStateText);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Load invitations
        loadEventInvites();
        addDebugButton();
    }

    /**
     * Temporary debug button to check Firestore data
     */
    private void addDebugButton() {
        Button debugButton = new Button(this);
        debugButton.setText("DEBUG: Check Firestore Data");
        debugButton.setOnClickListener(v -> debugFirestoreData());
        invitesContainer.addView(debugButton);
    }

    /**
     * Debug method to check what's actually in Firestore
     */
    private void debugFirestoreData() {
        String userEmail = session.getUserEmail();
        Log.d(TAG, "=== DEBUG: Checking Firestore for user: " + userEmail);

        // Check all events to see inviteList structure
        db.collection("events").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int eventsWithUserInInvites = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> inviteList = (Map<String, Object>) doc.get("inviteList");
                        if (inviteList != null && inviteList.containsKey(userEmail)) {
                            eventsWithUserInInvites++;
                            Log.d(TAG, "Found user in event: " + doc.getId());
                        }
                    }
                    Toast.makeText(this,
                            "DEBUG: Found " + eventsWithUserInInvites + " events with invites",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Debug failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Loads event invitations for the current user - FIXED VERSION
     */
    private void loadEventInvites() {
        String userEmail = session.getUserEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            showEmptyState("Please log in to view invitations");
            return;
        }
        Log.d(TAG, "Loading invitations for user: " + userEmail);

        // NEW APPROACH: Get all events and manually check if user is in inviteList Map
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " total events");

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState("No pending event invitations");
                        return;
                    }

                    invitesContainer.removeAllViews();
                    int invitationsFound = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Check if user is in the inviteList Map
                        Map<String, Object> inviteList = (Map<String, Object>) doc.get("inviteList");

                        if (inviteList != null && inviteList.containsKey(userEmail)) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setId(doc.getId());
                                addInvitationCard(event, userEmail);
                                invitationsFound++;
                                Log.d(TAG, "Added invitation for event: " + event.getEventName());
                            }
                        }
                    }

                    Log.d(TAG, "Total invitations found: " + invitationsFound);

                    if (invitationsFound == 0) {
                        showEmptyState("No pending event invitations");
                    } else {
                        emptyStateText.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event invitations", e);
                    showEmptyState("Failed to load invitations");
                });
    }

    /**
     * Loads event invitations for the current user
     */
//    private void loadEventInvites() {
//        String userEmail = session.getUserEmail();
//
//        if (userEmail == null || userEmail.isEmpty()) {
//            showEmptyState("Please log in to view invitations");
//            return;
//        }
//        Log.d(TAG, "Loading invitations for user: " + userEmail);
//
//        // Query events where this user is in the inviteList
//        db.collection("events")
//                .whereArrayContains("inviteList", userEmail)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    if (queryDocumentSnapshots.isEmpty()) {
//                        showEmptyState("No pending event invitations");
//                        return;
//                    }
//
//                    invitesContainer.removeAllViews();
//
//                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
//                        Event event = doc.toObject(Event.class);
//                        if (event != null) {
//                            event.setId(doc.getId());
//                            addInvitationCard(event, userEmail);
//                        }
//                    }
//
//                    emptyStateText.setVisibility(View.GONE);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Error loading event invitations", e);
//                    showEmptyState("Failed to load invitations");
//                });
//    }

    /**
     * Creates and adds an invitation card for an event
     */
    private void addInvitationCard(Event event, String userEmail) {
        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.notification_invite, invitesContainer, false);

        // Initialize views
        TextView tagTextView = cardView.findViewById(R.id.notificationTag);
        TextView titleTextView = cardView.findViewById(R.id.notificationTitle);
        TextView eventNameTextView = cardView.findViewById(R.id.notificationEventName);
        TextView organizerTextView = cardView.findViewById(R.id.notificationOrganizer);
        TextView messageTextView = cardView.findViewById(R.id.notificationMessage);
        Button acceptButton = cardView.findViewById(R.id.acceptButton);
        Button declineButton = cardView.findViewById(R.id.declineButton);

        // Set event data
        eventNameTextView.setText(event.getEventName());
        organizerTextView.setText("Organized by: " + event.getOrganizer().getEmail());
        messageTextView.setText("You have been invited to this event from the waitlist!");


        // Set up button listeners
        acceptButton.setOnClickListener(v -> {
            handleInvitationResponse(event.getId(), userEmail, true, cardView);
        });

        declineButton.setOnClickListener(v -> {
            handleInvitationResponse(event.getId(), userEmail, false, cardView);
        });

        invitesContainer.addView(cardView);
    }

    /**
     * Handles invitation response
     */
    private void handleInvitationResponse(String eventId, String userEmail, boolean accepted, View cardView) {
        // Disable buttons immediately
        Button acceptButton = cardView.findViewById(R.id.acceptButton);
        Button declineButton = cardView.findViewById(R.id.declineButton);

        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);

        lotteryService.handleInvitationResponse(eventId, userEmail, accepted,
                new LotteryService.InvitationResponseCallback() {
                    @Override
                    public void onResponseSuccess(boolean accepted) {
                        runOnUiThread(() -> {
                            String status = accepted ? "✓ Invitation Accepted" : "✗ Invitation Declined";


                            // Remove card after a delay
                            new Handler().postDelayed(() -> {
                                invitesContainer.removeView(cardView);
                                // Check if container is empty
                                if (invitesContainer.getChildCount() == 0) {
                                    showEmptyState("No pending event invitations");
                                }
                            }, 2000);
                        });
                    }

                    @Override
                    public void onResponseFailed(Exception exception) {
                        runOnUiThread(() -> {
                            acceptButton.setEnabled(true);
                            declineButton.setEnabled(true);
                        });
                    }
                });
    }
    /**
     * Disables buttons and updates status after response
     */
    private void disableButtons(Button acceptButton, Button declineButton, TextView statusText, String status) {
        acceptButton.setEnabled(false);
        declineButton.setEnabled(false);
        statusText.setText(status);
        statusText.setTextColor(ContextCompat.getColor(this,
                status.contains("Accepted") ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    }

    private void showEmptyState(String message) {
        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
        invitesContainer.removeAllViews();
    }
}