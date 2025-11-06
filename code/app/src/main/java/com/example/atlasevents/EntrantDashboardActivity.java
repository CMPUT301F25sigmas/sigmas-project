package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Activity displaying the entrant's dashboard with a list of available events.
 * <p>
 * This activity extends {@link EntrantBase} to provide the navigation sidebar and
 * displays all events retrieved from Firebase. Events are shown as cards that users
 * can tap to view detailed information. The activity handles fetching events from
 * the repository and dynamically creating event card views.
 * </p>
 *
 * @see EntrantBase
 * @see Event
 * @see EventRepository
 * @see EventDetailsActivity
 */
public class EntrantDashboardActivity extends EntrantBase {

    /**
     * Container layout that holds all event card views.
     */
    private LinearLayout eventsContainer;

    /**
     * Repository for fetching event data from Firebase.
     */
    private EventRepository eventRepository;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the dashboard layout, sets up the events container, creates
     * the event repository instance, and triggers loading of events from Firebase.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied in onSaveInstanceState.
     *                           Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_dashboard);

        eventsContainer = findViewById(R.id.events_container);
        eventRepository = new EventRepository();

        loadEventsFromFirebase();
    }

    /**
     * Fetches all events from Firebase and displays them.
     * <p>
     * Makes an asynchronous call to the event repository to retrieve all available
     * events. On success, the events are passed to {@link #displayEvents(ArrayList)}
     * for rendering. On failure, the error is handled silently (currently no-op).
     * </p>
     */
    private void loadEventsFromFirebase() {
        // Fetch events from Firebase
        eventRepository.getAllEvents(new EventRepository.EventsCallback(){
            @Override
            public void onSuccess(ArrayList<Event> events) {
                displayEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error - maybe show empty state
            }
        });
    }

    /**
     * Displays a list of events as card views in the events container.
     * <p>
     * Clears any existing event cards and dynamically inflates new card views
     * for each event in the list. Each card shows the event name and image
     * (image loading not yet implemented), and is clickable to open event details.
     * </p>
     *
     * @param events The list of events to display
     */
    private void displayEvents(ArrayList<Event> events) {
        eventsContainer.removeAllViews(); // Clear any existing views

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            // Inflate the event card layout
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);

            // Get references to views in the card
            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);

            // Set event data
            // TODO: Implement loadImage to Load event image from Firebase Storage
            //event.loadImage(eventImage);
            eventName.setText(event.getEventName());


            eventCard.setOnClickListener(v -> openEventDetails(event));

            eventsContainer.addView(eventCard);
        }
    }
    /**
     * Opens the event details screen for the specified event.
     * <p>
     * Launches {@link EventDetailsActivity} and passes the selected event
     * as an extra in the intent for display.
     * </p>
     *
     * @param event The event to display details for
     */
    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event);
        startActivity(intent);
    }
}