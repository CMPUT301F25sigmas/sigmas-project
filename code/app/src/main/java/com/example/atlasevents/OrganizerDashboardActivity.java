package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

public class OrganizerDashboardActivity extends OrganizerBase {

    private EventRepository eventRepository;
    private LinearLayout eventsContainer;
    private ScrollView eventsScrollView;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_dashboard_empty);
        eventsContainer = findViewById(R.id.events_container_organizer);
        Button createEventButton = findViewById(R.id.create_event_button);
        eventRepository = new EventRepository();
        eventsScrollView = findViewById(R.id.events_scroll_view);
        emptyState = findViewById(R.id.empty_state);
        createEventButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
        loadOrganizerEvents();
    }
    /**
     * Loads all events created by the currently logged-in organizer from Firestore.
     * Retrieves the organizer information using the session email, then queries for their events.
     * Displays events if found, otherwise shows the empty state.
     */
    private void loadOrganizerEvents() {
        userRepository.getOrganizer(session.getUserEmail(), organizer -> {
            if (organizer != null) {
                eventRepository.getEventsByOrganizer(organizer.getEmail(), new EventRepository.EventsCallback() {
                    @Override
                    public void onSuccess(ArrayList<Event> events) {
                        if (events.isEmpty()) {
                            showEmptyState();
                        } else {
                            displayEvents(events);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showEmptyState();
                    }
                });
            } else {
                showEmptyState();
            }
        });
    }

    /**
     * Displays a list of events as clickable cards in the events container.
     * Hides the empty state and shows the scrollable events list.
     * Each event card displays the event name and image, and navigates to event details when clicked.
     *
     * @param events ArrayList of Event objects to display
     */
    private void displayEvents(ArrayList<Event> events) {
        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);

        eventsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);

            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);

            //event.loadImage(eventImage, R.drawable.event_placeholder1);
            eventName.setText(event.getEventName());

            eventCard.setOnClickListener(v -> openEventDetails(event));
            eventsContainer.addView(eventCard);
        }
    }

    /**
     * Shows the empty state layout with a message and create event button.
     * Hides the events scroll view.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        eventsScrollView.setVisibility(View.GONE);
    }
    /**
     * Opens the EventDetailsActivity for a specific event.
     * Passes the event object as an extra in the intent.
     *
     * @param event The Event object to view details for
     */
    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event);
        startActivity(intent);
    }
}
