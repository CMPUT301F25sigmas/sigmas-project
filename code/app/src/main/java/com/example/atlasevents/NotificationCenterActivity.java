package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.atlasevents.data.model.Notification;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.atlasevents.User;
import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Notification Center - Where organizers pick which event to send notifications for.
 * 
 * This activity shows a list of all events that the current organizer has created.
 * When they tap on an event, it opens the ComposeNotificationActivity where they
 * can write and send notifications to that event's entrants.
 * 
 * Acts like "main menu" for the notification system - it lets organizers
 * choose which event they want to send notifications about.
 * 
 * @author CMPUT301F25sigmas
 */
public class NotificationCenterActivity extends AppCompatActivity {
    /**
     * Tag for logging
     */
    private static final String TAG = "NotificationCenter";

    /**
     * Container that holds all the event cards
     */
    private LinearLayout eventsContainer;
    /**
     * View shown when the organizer has no events
     */
    private LinearLayout emptyView;
    /**
     * Repository for loading events from Firebase
     */
    private EventRepository eventRepository;
    /**
     * Session manager to get current organizer's email
     */
    private Session session;

    /**
     * Called when the activity is created.
     * Sets up the UI and loads the organizer's events from Firebase.
     *
     * @param savedInstanceState Bundle containing saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_center);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase and session
        session = new Session(this);
        eventRepository = new EventRepository();

        // Get UI elements
        eventsContainer = findViewById(R.id.events_container);
        emptyView = findViewById(R.id.empty_view_notification);
        Button notificationHistoryButton = findViewById(R.id.notificationHistory);
        ImageButton backButton = findViewById(R.id.notificationCentreBackButton);

        // Button to view notification history (for organizers to see what they've sent)
        notificationHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationHistoryActivity.class);
            startActivity(intent);
        });

        // Back button
        backButton.setOnClickListener(view ->{
            finish();
        });

        // Load and display all events for this organizer
        loadOrganizerEvents();
    }

    /**
     * Loads all events that belong to the current organizer from Firebase.
     * <p>
     * This method:
     * 1. Gets all events from the database
     * 2. Filters them to only show events where the organizer's email matches
     * 3. Shows either the event list or an empty state message
     */
    private void loadOrganizerEvents() {
        // Get the organizer's email from the session
        String organizerEmail = session.getUserEmail();

        // If no email, can't load events
        if (organizerEmail == null || organizerEmail.isEmpty()) {
            Log.e(TAG, "No organizer email found");
            showEmptyView();
            return;
        }

        // Fetch all events from Firebase
        eventRepository.getAllEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                // Filter to only show this organizer's events
                ArrayList<Event> organizerEvents = new ArrayList<>();

                for (Event event : events) {
                    // Check if this event belongs to the current organizer
                    if (event.getOrganizer() != null &&
                            event.getOrganizer().getEmail() != null &&
                            event.getOrganizer().getEmail().equals(organizerEmail)) {
                        organizerEvents.add(event);
                    }
                }

                // Show either the events or an empty message
                if (organizerEvents.isEmpty()) {
                    showEmptyView();
                } else {
                    showEventsList(organizerEvents);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Something went wrong loading events
                Log.e(TAG, "Failed to load events", e);
                showEmptyView();
            }
        });
    }

    /**
     * Displays the list of events as clickable cards.
     * Each card shows the event name and opens ComposeNotificationActivity when clicked.
     *
     * @param events List of events to display
     */
    private void showEventsList(ArrayList<Event> events) {
        // Hide empty view, show events container
        emptyView.setVisibility(View.GONE);
        eventsContainer.setVisibility(View.VISIBLE);
        eventsContainer.removeAllViews(); // Clear any old cards

        LayoutInflater inflater = LayoutInflater.from(this);

        // Create a card for each event
        for (Event event : events) {
            // Inflate the event card layout
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);

            // Set the event name on the card
            TextView eventName = eventCard.findViewById(R.id.event_name);
            eventName.setText(event.getEventName());
            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            if (!event.getImageUrl().isEmpty()) {
                Glide.with(this).load(event.getImageUrl()).into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.poster);
            }

            // When card is clicked, open the notification composer for this event
            eventCard.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationCenterActivity.this, ComposeNotificationActivity.class);
                // Pass event info to the next activity
                intent.putExtra("eventId", event.getId());
                intent.putExtra("eventName", event.getEventName());
                startActivity(intent);
            });

            // Add the card to the screen
            eventsContainer.addView(eventCard);
        }
    }

    /**
     * Shows the empty state view when the organizer has no events.
     * This hides the events list and shows a message instead.
     */
    private void showEmptyView() {
        eventsContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
}
