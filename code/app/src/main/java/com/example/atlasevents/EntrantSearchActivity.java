package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Activity that allows entrants to browse and search through available events.
 * <p>
 * This screen displays all events fetched from Firebase, showing them as scrollable
 * cards containing event images and names. When an event is tapped, it navigates
 * to {@link EventDetailsActivity} to display detailed information about that event.
 * </p>
 *
 * <p>
 * If no events are found, an empty state view is shown to inform the user that there
 * are currently no available events.
 * </p>
 *
 * @see Event
 * @see EventDetailsActivity
 * @see EventRepository
 */
public class EntrantSearchActivity extends EntrantBase {

    /** Container layout that holds dynamically added event cards. */
    private LinearLayout eventsContainer;

    /** Repository used for retrieving event data from Firebase. */
    private EventRepository eventRepository;

    /** Scroll view that wraps the event list to allow scrolling through multiple events. */
    private ScrollView eventsScrollView;

    /** Layout displayed when no events are available to show. */
    private LinearLayout emptyState;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the layout, sets up the event container, and triggers
     * loading of events from Firebase. Initially hides both the scroll view
     * and empty state until data is retrieved.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied. Otherwise it is {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_search);
        setActiveNavItem(R.id.search_icon_card);

        eventsContainer = findViewById(R.id.events_container_organizer);
        eventRepository = new EventRepository();

        eventsScrollView = findViewById(R.id.events_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.GONE);

        loadEventsFromFirebase();
    }

    /**
     * Retrieves all events from Firebase using {@link EventRepository}.
     * <p>
     * If events are successfully fetched, they are displayed using
     * {@link #displayEvents(ArrayList)}. If the list is empty or
     * the request fails, {@link #showEmptyState()} is called instead.
     * </p>
     */
    private void loadEventsFromFirebase() {
        eventRepository.getAllEvents(new EventRepository.EventsCallback() {
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
    }

    /**
     * Displays a list of events as scrollable cards.
     * <p>
     * Each event card contains an image and name. When an event is tapped,
     * {@link #openEventDetails(Event)} is called to navigate to the event’s details screen.
     * </p>
     *
     * @param events The list of events to display.
     */
    private void displayEvents(ArrayList<Event> events) {
        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);
        eventsContainer.removeAllViews(); // Clear existing event cards

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            // Inflate the event card layout
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);

            // Get references to views in the card
            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);

            // Set event data
            if (!event.getImageUrl().isEmpty()) {
                Glide.with(this).load(event.getImageUrl()).into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.poster);
            }
            eventName.setText(event.getEventName());

            // Open event details on click
            eventCard.setOnClickListener(v -> openEventDetails(event));

            // Add the card to the container
            eventsContainer.addView(eventCard);
        }
    }

    /**
     * Opens the {@link EventDetailsActivity} for the selected event.
     * <p>
     * The event ID is passed as an Intent extra using
     * {@link EventDetailsActivity#EventKey}.
     * </p>
     *
     * @param event The selected event to display details for.
     */
    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    /**
     * Called when the activity becomes visible again.
     * <p>
     * Ensures that the event list is refreshed in case new events were added
     * while the user was viewing details or another screen.
     * </p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEventsFromFirebase();
    }

    /**
     * Displays the empty state layout when no events are available.
     * <p>
     * This method hides the scrollable events list and shows the
     * placeholder layout encouraging users to check back later.
     * </p>
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        eventsScrollView.setVisibility(View.GONE);
    }
}
