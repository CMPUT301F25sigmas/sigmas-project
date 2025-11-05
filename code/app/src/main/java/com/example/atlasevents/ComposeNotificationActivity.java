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
 * Activity for composing and sending notifications to event entrants
 */
public class ComposeNotificationActivity extends AppCompatActivity {
    private static final String TAG = "ComposeNotification";
    
    private String eventId;
    private String eventName;
    private Event currentEvent;
    
    private RadioButton radioWaitingList;
    private RadioButton radioChosenEntrants;
    private RadioButton radioCancelledEntrants;
    
    private Button waitingListCountBtn;
    private Button chosenCountBtn;
    private Button cancelledCountBtn;
    
    private EditText messageEditText;
    private TextView characterCountTextView;
    private Button sendButton;
    
    private EventRepository eventRepository;
    private NotificationRepository notificationRepository;
    private Session session;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_notifications);
        
        // Initialize
        session = new Session(this);
        eventRepository = new EventRepository();
        notificationRepository = new NotificationRepository();
        
        // Get event info from intent
        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        
        // Initialize views
        initializeViews();
        
        // Setup listeners
        setupListeners();
        
        // Load event data
        loadEventData();
    }
    
    /**
     * Initialize all views
     */
    private void initializeViews() {
        // Back button
        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> finish());
        
        // Radio buttons
        radioWaitingList = findViewById(R.id.radioWaitingList);
        radioChosenEntrants = findViewById(R.id.radioChosenEntrants);
        radioCancelledEntrants = findViewById(R.id.radioCancelledEntrants);
        
        // Count buttons
        waitingListCountBtn = findViewById(R.id.waitingListCount);
        chosenCountBtn = findViewById(R.id.chosenCount);
        cancelledCountBtn = findViewById(R.id.cancelledCount);
        
        // Message input
        messageEditText = findViewById(R.id.messageEditText);
        characterCountTextView = findViewById(R.id.characterCount);
        
        // Send button
        sendButton = findViewById(R.id.sendNotificationButton);
    }
    
    /**
     * Setup all listeners
     */
    private void setupListeners() {
        // Radio button group behavior
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
        
        // Character counter
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                characterCountTextView.setText(s.length() + " characters");
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Send button
        sendButton.setOnClickListener(v -> sendNotification());
    }
    
    /**
     * Load event data and populate counts
     */
    private void loadEventData() {
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "No event ID provided");
            Toast.makeText(this, "Error: No event selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                updateCounts(event);
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load event", e);
                Toast.makeText(ComposeNotificationActivity.this, 
                             "Failed to load event data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Update the count buttons with actual counts from the event
     */
    private void updateCounts(Event event) {
        // Get counts from event's entrant lists
        int waitingCount = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        int chosenCount = event.getInviteList() != null ? event.getInviteList().size() : 0;
        int cancelledCount = event.getDeclinedList() != null ? event.getDeclinedList().size() : 0;
        
        waitingListCountBtn.setText(String.valueOf(waitingCount));
        chosenCountBtn.setText(String.valueOf(chosenCount));
        cancelledCountBtn.setText(String.valueOf(cancelledCount));
    }
    
    /**
     * Send notification to selected recipient group
     */
    private void sendNotification() {
        String message = messageEditText.getText().toString().trim();
        
        // Validate message
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Determine which group to send to
        String title;
        if (radioWaitingList.isChecked()) {
            title = "Update about " + eventName;
            notificationRepository.sendToWaitlist(currentEvent, title, message)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notification sent to waiting list", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send notification", e);
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                });
        } else if (radioChosenEntrants.isChecked()) {
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
            Toast.makeText(this, "Please select a recipient group", Toast.LENGTH_SHORT).show();
        }
    }
}
