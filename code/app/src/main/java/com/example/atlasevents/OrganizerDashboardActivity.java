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

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;


/**
 * Activity displaying the organizer's dashboard with all their created events.
 * <p>
 * This activity extends {@link OrganizerBase} and provides a view of all events
 * created by the currently logged-in organizer. Events are displayed as clickable
 * cards that navigate to detailed event information. If no events exist, an empty
 * state with a create event button is shown.
 * </p>
 *
 * @see OrganizerBase
 * @see Event
 * @see EventRepository
 */
public class OrganizerDashboardActivity extends OrganizerBase {

    /**
     * Repository for accessing and managing event data from Firestore.
     */
    private EventRepository eventRepository;

    /**
     * Container layout that holds individual event card views.
     */
    private LinearLayout eventsContainer;

    /**
     * Scrollable view containing the events container.
     */
    private ScrollView eventsScrollView;

    /**
     * Layout displayed when the organizer has no events.
     */
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

        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.GONE);

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
            View eventCard = inflater.inflate(R.layout.organizer_event_cards, eventsContainer, false);

            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);
            Button eventEditButton = eventCard.findViewById(R.id.edit_button);

            if(!event.getImageUrl().isEmpty()){
                Glide.with(this).load(event.getImageUrl()).into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.poster);
            }
            eventName.setText(event.getEventName());

            eventCard.setOnClickListener(v -> openEventManage(event));
            eventEditButton.setOnClickListener(v -> openEventEdit(event));
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
    private void openEventEdit(Event event) {
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    /**
     * Opens the EventManageActivity for a specific event.
     * Passes the event object as an extra in the intent.
     *
     * @param event The Event object to manage
     */
    private void openEventManage(Event event) {
        System.out.println("I failed here");
        Intent intent = new Intent(this, EventManageActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    /**
     * Resumes the activity and reloads the organizer's events.
     */

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }
}
