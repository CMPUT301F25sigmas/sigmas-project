package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Activity that shows all active events for the organizer to select 
 * which event they want to send notifications for
 */
public class NotificationCenterActivity extends AppCompatActivity {
    private static final String TAG = "NotificationCenter";
    
    private LinearLayout eventsContainer;
    private LinearLayout emptyView;
    private EventRepository eventRepository;
    private Session session;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_center);
        
        // Initialize
        session = new Session(this);
        eventRepository = new EventRepository();
        
        // Get references
        eventsContainer = findViewById(R.id.events_container);
        emptyView = findViewById(R.id.empty_view_notification);
        Button notificationHistoryButton = findViewById(R.id.notificationHistory);
        
        // Set up notification history button
        notificationHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationHistoryActivity.class);
            startActivity(intent);
        });
        
        // Load organizer's events
        loadOrganizerEvents();
    }
    
    /**
     * Load all active events for the current organizer
     */
    private void loadOrganizerEvents() {
        String organizerEmail = session.getUserEmail();
        
        if (organizerEmail == null || organizerEmail.isEmpty()) {
            Log.e(TAG, "No organizer email found");
            showEmptyView();
            return;
        }
        
        // Load all events and filter by organizer
        eventRepository.getAllEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                ArrayList<Event> organizerEvents = new ArrayList<>();
                
                // Filter events by organizer email
                for (Event event : events) {
                    // Check if event has organizer and if organizer email matches
                    if (event.getOrganizer() != null && 
                        event.getOrganizer().getEmail() != null &&
                        event.getOrganizer().getEmail().equals(organizerEmail)) {
                        organizerEvents.add(event);
                    }
                }
                
                if (organizerEvents.isEmpty()) {
                    showEmptyView();
                } else {
                    showEventsList(organizerEvents);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load events", e);
                showEmptyView();
            }
        });
    }
    
    /**
     * Show the list of events
     */
    private void showEventsList(ArrayList<Event> events) {
        emptyView.setVisibility(View.GONE);
        eventsContainer.setVisibility(View.VISIBLE);
        eventsContainer.removeAllViews(); // Clear any existing views
        
        LayoutInflater inflater = LayoutInflater.from(this);
        
        // Add each event as a card
        for (Event event : events) {
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);
            
            // Set event name
            TextView eventName = eventCard.findViewById(R.id.event_name);
            eventName.setText(event.getEventName());
            
            // Set click listener to open compose notification
            eventCard.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationCenterActivity.this, ComposeNotificationActivity.class);
                intent.putExtra("eventId", event.getId());
                intent.putExtra("eventName", event.getEventName());
                startActivity(intent);
            });
            
            eventsContainer.addView(eventCard);
        }
    }
    
    /**
     * Show empty view when no events exist
     */
    private void showEmptyView() {
        eventsContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
}
