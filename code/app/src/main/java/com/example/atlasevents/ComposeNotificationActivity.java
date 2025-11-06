package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.NotificationRepository;

/**
 * Activity where organizers compose and send notifications to entrants of a specific event.
 * 
 * This screen lets organizers:
 * - Choose which group to notify (waiting list, chosen/invited, or cancelled)
 * - See how many people are in each group
 * - Write a custom message
 * - Send the notification to everyone in the selected group
 * 
 * The activity gets event information from the previous screen (NotificationCenterActivity)
 * and loads the full event details to show accurate counts for each entrant list.
 * 
 * @author CMPUT301F25sigmas
 */
public class ComposeNotificationActivity extends AppCompatActivity {
    /** Tag for logging -  for debugging */
    private static final String TAG = "ComposeNotification";
    
    // Event information passed from previous activity
    /** The unique ID of the event we're sending notifications for */
    private String eventId;
    /** The name of the event (shown in notification title) */
    private String eventName;
    /** The full event object loaded from Firebase with all entrant lists */
    private Event currentEvent;
    
    // Radio buttons for selecting recipient group
    /** Radio button to select waiting list as recipients */
    private RadioButton radioWaitingList;
    /** Radio button to select chosen/invited entrants as recipients */
    private RadioButton radioChosenEntrants;
    /** Radio button to select cancelled entrants as recipients */
    private RadioButton radioCancelledEntrants;
    
    // Count display buttons (show how many people in each group)
    /** Button showing number of people on waiting list */
    private Button waitingListCountBtn;
    /** Button showing number of chosen entrants */
    private Button chosenCountBtn;
    /** Button showing number of cancelled entrants */
    private Button cancelledCountBtn;
    
    // Message composition UI elements
    /** Text field where organizer types the notification message */
    private EditText messageEditText;
    /** Shows character count as user types */
    private TextView characterCountTextView;
    /** Button to send the notification */
    private Button sendButton;
    
    // Firebase helpers
    /** Repository for loading event data from Firestore */
    private EventRepository eventRepository;
    /** Repository for sending notifications to users */
    private NotificationRepository notificationRepository;
    /** Session manager to get current user info */
    private Session session;
    
    /**
     * Called when the activity is first created.
     * Sets up the UI, gets event info from the intent, and loads event data from Firebase.
     * 
     * @param savedInstanceState Bundle containing saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_notifications);
        
        // Initialize Firebase repositories and session
        session = new Session(this);
        eventRepository = new EventRepository();
        notificationRepository = new NotificationRepository();
        
        // Get event info that was passed from NotificationCenterActivity
        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        
        // Set up all the UI elements
        initializeViews();
        
        // Attach click listeners to buttons and radio buttons
        setupListeners();
        
        // Load the full event data from Firebase to get entrant counts
        loadEventData();
    }
    
    /**
     * Finds and initializes all UI components from the layout.
     * separated into its own method to keep onCreate() clean and readable.
     */
    private void initializeViews() {
        // Back button - goes back to event selection screen
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());
        
        // Radio buttons for choosing recipient group
        radioWaitingList = findViewById(R.id.radioWaitingList);
        radioChosenEntrants = findViewById(R.id.radioChosenEntrants);
        radioCancelledEntrants = findViewById(R.id.radioCancelledEntrants);
        
        // Buttons that show counts (not clickable, just display numbers)
        waitingListCountBtn = findViewById(R.id.waitingListCount);
        chosenCountBtn = findViewById(R.id.chosenCount);
        cancelledCountBtn = findViewById(R.id.cancelledCount);
        
        // Message input field and character counter
        messageEditText = findViewById(R.id.messageEditText);
        characterCountTextView = findViewById(R.id.characterCount);
        
        // Send button
        sendButton = findViewById(R.id.sendNotificationButton);
    }
    
    /**
     * Sets up all the click listeners and text watchers for UI elements.
     * 
     * The radio buttons are set up to act as a group (only one can be selected at a time).
     * The text field has a character counter that updates as you type.
     */
    private void setupListeners() {
        // Radio button group behavior - when you click one, uncheck the others
        // This ensures only one group can be selected at a time
        radioWaitingList.setOnClickListener(v -> {
            radioChosenEntrants.setChecked(false);
            radioCancelledEntrants.setChecked(false);
        });
        
        radioChosenEntrants.setOnClickListener(v -> {
            radioWaitingList.setChecked(false);
            radioCancelledEntrants.setChecked(false);
        });
        
        radioCancelledEntrants.setOnClickListener(v -> {
            radioWaitingList.setChecked(false);
            radioChosenEntrants.setChecked(false);
        });
        
        // Character counter - updates in real-time as user types
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for character counting (dont delete, needed for TextWatcher() to work)
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update the character count display
                characterCountTextView.setText(s.length() + " characters");
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for character counting (dont delete, needed for TextWatcher() to work)
            }
        });
        
        // Send button - validates input and sends notification
        sendButton.setOnClickListener(v -> sendNotification());
    }
    
    /**
     * Loads the event data from Firebase to get accurate entrant list counts.
     * If the event ID is missing or invalid, shows an error and closes the activity.
     */
    private void loadEventData() {
        // Validate that we have an event ID
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "No event ID provided");
            Toast.makeText(this, "Error: No event selected", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity since we can't do anything without an event
            return;
        }
        
        // Fetch the event from Firebase
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                // Store the event and update the count displays
                currentEvent = event;
                updateCounts(event);
            }
            
            @Override
            public void onFailure(Exception e) {
                // Failed to load event - show error message
                Log.e(TAG, "Failed to load event", e);
                Toast.makeText(ComposeNotificationActivity.this, 
                             "Failed to load event data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Updates the count buttons to show how many people are in each entrant list.
     * 
     * @param event The event object containing all entrant lists
     */
    private void updateCounts(Event event) {
        // Get the size of each list (handle null lists gracefully)
        int waitingCount = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        int chosenCount = event.getInviteList() != null ? event.getInviteList().size() : 0;
        int cancelledCount = event.getDeclinedList() != null ? event.getDeclinedList().size() : 0;
        
        // Update the count displays
        waitingListCountBtn.setText(String.valueOf(waitingCount));
        chosenCountBtn.setText(String.valueOf(chosenCount));
        cancelledCountBtn.setText(String.valueOf(cancelledCount));
    }
    
    /**
     * Validates the input and sends the notification to the selected recipient group.
     * 
     * This method:
     * 1. Checks that a message was entered
     * 2. Checks that event data is loaded
     * 3. Determines which group is selected
     * 4. Calls the appropriate repository method to send the notification
     * 5. Shows success/error messages and closes the activity on success
     */
    private void sendNotification() {
        // Get and validate the message
        String message = messageEditText.getText().toString().trim();
        
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Make sure event data is loaded
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determine which group to send to based on which radio button is checked
        String title;
        if (radioWaitingList.isChecked()) {
            // Sending to waiting list
            title = "Update about " + eventName;
            notificationRepository.sendToWaitlist(currentEvent, title, message)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notification sent to waiting list", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity and go back
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
                
        } else if (radioChosenEntrants.isChecked()) {
            // Sending to chosen/invited entrants
            title = "You've been selected!";
            notificationRepository.sendToInvited(currentEvent, title, message)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notification sent to chosen entrants", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
                
        } else if (radioCancelledEntrants.isChecked()) {
            // Sending to cancelled entrants
            title = "Event Update: " + eventName;
            notificationRepository.sendToCancelled(currentEvent, title, message)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notification sent to cancelled entrants", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
                
        } else {
            // No group selected - show error
            Toast.makeText(this, "Please select a recipient group", Toast.LENGTH_SHORT).show();
        }
    }
}
