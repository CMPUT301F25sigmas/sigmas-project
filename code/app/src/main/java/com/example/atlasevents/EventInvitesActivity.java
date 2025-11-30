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
import com.example.atlasevents.data.InviteRepository;
import com.example.atlasevents.data.model.Invite;
import com.example.atlasevents.data.EventRepository;
import com.google.android.gms.tasks.Task;
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
    private InviteRepository inviteRepo;
    private EventRepository eventRepository;
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
        inviteRepo = new InviteRepository();
        eventRepository = new EventRepository();

        // Setup views
        invitesContainer = findViewById(R.id.invitesContainer);
        emptyStateText = findViewById(R.id.emptyStateText);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Add debug button to check Firestore data
        addDebugButton();

        // Load invitations
        loadEventInvites();
    }
    
    /**
     * Temporary debug method to check what's in Firestore
     */
    private void addDebugButton() {
        // Only add in debug builds or remove this after fixing
        Button debugButton = new Button(this);
        debugButton.setText("DEBUG: Check Firestore");
        debugButton.setOnClickListener(v -> debugFirestoreInvites());
        // Uncomment to add button to UI for debugging
        invitesContainer.addView(debugButton);
    }
    
    /**
     * Debug method to check what invites exist in Firestore
     */
    private void debugFirestoreInvites() {
        String userEmail = session.getUserEmail();
        Log.d(TAG, "=== DEBUG: Checking Firestore invites for: " + userEmail);
        
        // Check all invites for this user
        db.collection("invites")
                .whereEqualTo("recipientEmail", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Total invites found: " + queryDocumentSnapshots.size());
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d(TAG, "Invite ID: " + doc.getId());
                        Log.d(TAG, "  Event ID: " + doc.get("eventId"));
                        Log.d(TAG, "  Status: " + doc.get("status"));
                        Log.d(TAG, "  Event Name: " + doc.get("eventName"));
                        Log.d(TAG, "  Created At: " + doc.get("createdAt"));
                        Log.d(TAG, "  Expiration Time: " + doc.get("expirationTime"));
                    }
                    Toast.makeText(this, 
                            "DEBUG: Found " + queryDocumentSnapshots.size() + " invites. Check Logcat.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "DEBUG: Failed to query invites", e);
                    Toast.makeText(this, "DEBUG failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    /**
     * Loads event invitations for the current user from the invites collection
     */
    private void loadEventInvites() {
        String userEmail = session.getUserEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            showEmptyState("Please log in to view invitations");
            return;
        }
        Log.d(TAG, "Loading invitations for user: " + userEmail);

        // Load invites from the invites collection
        inviteRepo.getPendingInvitesForUser(userEmail)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        Log.e(TAG, "Error loading event invitations", exception);
                        
                        // Log detailed error information
                        if (exception != null) {
                            Log.e(TAG, "Exception message: " + exception.getMessage());
                            Log.e(TAG, "Exception class: " + exception.getClass().getName());
                            if (exception.getCause() != null) {
                                Log.e(TAG, "Cause: " + exception.getCause().getMessage());
                            }
                            
                            // Check if it's a Firestore index error
                            String errorMsg = exception.getMessage();
                            if (errorMsg != null && errorMsg.contains("index")) {
                                Log.e(TAG, "FIRESTORE INDEX REQUIRED! Check Logcat for index creation URL");
                                showEmptyState("Database index required. Please check logs for setup instructions.");
                            } else {
                                showEmptyState("Failed to load invitations: " + exception.getMessage());
                            }
                        } else {
                            showEmptyState("Failed to load invitations");
                        }
                        return;
                    }

                    List<Invite> invites = task.getResult();
                    Log.d(TAG, "Loaded " + (invites != null ? invites.size() : 0) + " invites");
                    
                    if (invites == null || invites.isEmpty()) {
                        showEmptyState("No pending event invitations");
                        return;
                    }

                    invitesContainer.removeAllViews();

                    // Load event details for each invite
                    for (Invite invite : invites) {
                        loadEventForInvite(invite);
                    }
                });
    }

    /**
     * Loads event details for an invite and displays the invitation card
     */
    private void loadEventForInvite(Invite invite) {
        eventRepository.getEventById(invite.getEventId(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    event.setId(invite.getEventId()); // Ensure event ID is set
                    addInvitationCard(event, invite);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load event for invite: " + invite.getEventId(), e);
                // Still show the invite even if event loading fails
                addInvitationCardFromInvite(invite);
            }
        });
    }


    /**
     * Creates and adds an invitation card for an event
     */
    private void addInvitationCard(Event event, Invite invite) {
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
        if (titleTextView != null) {
            titleTextView.setText("Event Invitation: " + invite.getEventName());
        }
        eventNameTextView.setText(invite.getEventName());
        organizerTextView.setText("Organized by: " + invite.getOrganizerEmail());
        messageTextView.setText(invite.getMessage() != null ? invite.getMessage() : 
                "You have been invited to this event from the waitlist!");

        // Set up button listeners
        acceptButton.setOnClickListener(v -> {
            handleInvitationResponse(event.getId(), invite.getRecipientEmail(), true, cardView);
        });

        declineButton.setOnClickListener(v -> {
            handleInvitationResponse(event.getId(), invite.getRecipientEmail(), false, cardView);
        });

        invitesContainer.addView(cardView);
    }

    /**
     * Creates and adds an invitation card from invite data only (when event loading fails)
     */
    private void addInvitationCardFromInvite(Invite invite) {
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

        // Set invite data
        if (titleTextView != null) {
            titleTextView.setText("Event Invitation: " + invite.getEventName());
        }
        eventNameTextView.setText(invite.getEventName());
        organizerTextView.setText("Organized by: " + invite.getOrganizerEmail());
        messageTextView.setText(invite.getMessage() != null ? invite.getMessage() : 
                "You have been invited to this event from the waitlist!");

        // Set up button listeners
        acceptButton.setOnClickListener(v -> {
            handleInvitationResponse(invite.getEventId(), invite.getRecipientEmail(), true, cardView);
        });

        declineButton.setOnClickListener(v -> {
            handleInvitationResponse(invite.getEventId(), invite.getRecipientEmail(), false, cardView);
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


                            // Reload invites to refresh the list
                            new Handler().postDelayed(() -> {
                                loadEventInvites();
                            }, 1000);
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