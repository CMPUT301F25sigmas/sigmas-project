package com.example.atlasevents;

import android.content.Intent;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.utils.NotificationManager;

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
    private Session session;


    /**
     * Scroll view containing the list of events.
     */
    private ScrollView eventsScrollView;

    /**
     * Layout for displaying a message when no events are available.
     */
    private LinearLayout emptyState;

    private Button currentButton, pastButton;
    private ArrayList<Event> userEvents = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_dashboard);
        setActiveNavItem(R.id.events_icon_card);

        eventsContainer = findViewById(R.id.events_container_organizer);
        eventRepository = new EventRepository();
        session = new Session(this);

        // Set up notification icon click listener
//        findViewById(R.id.notifications_icon).setOnClickListener(v -> {
//            Intent intent = new Intent(this, NotificationHistoryActivity.class);
//            startActivity(intent);
//        });

        currentButton = findViewById(R.id.filterCurrentButton);
        pastButton = findViewById(R.id.filterPastButton);

        currentButton.setOnClickListener(v -> filterEvents(true));
        pastButton.setOnClickListener(v -> filterEvents(false));

        eventsScrollView = findViewById(R.id.events_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.GONE);

        loadEventsFromFirebase();
    }

    /***
     * listener for notifications added to event dashboard as this is the foreground/ main activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        NotificationManager.startListening(this, session.getUserEmail());
        loadEventsFromFirebase();
    }
    /***
     * listener for notifications added to event dashboard as this is the foreground/ main activity
     */

    @Override
    protected void onPause() {
        NotificationManager.stopListening();
        super.onPause();
    }


    /**
     * Fetches all events from Firebase and displays them.
     * <p>
     * Makes an asynchronous call to the event repository to retrieve all available
     * events. On success, the events are passed to {@link #displayEvents(ArrayList)}
     * for rendering. On failure, {@link #showEmptyState()} is called to show an
     * empty state layout.
     * </p>
     */
    private void loadEventsFromFirebase() {
        // Fetch events from Firebase
        eventRepository.getEventsByEntrant(session.getUserEmail(), new EventRepository.EventsCallback(){
            @Override
            public void onSuccess(ArrayList<Event> events) {
                if (events.isEmpty()) {
                    showEmptyState();
                } else {
                    userEvents.clear();
                    userEvents.addAll(events);
                    displayEvents(userEvents);
                }
            }

            @Override
            public void onFailure(Exception e) {
                showEmptyState();
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
        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);
        eventsContainer.removeAllViews(); // Clear any existing views

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            // Inflate the event card layout
            View eventCard = inflater.inflate(R.layout.event_card_item, eventsContainer, false);

            // Get references to views in the card
            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);

            // Set event data
            if(!event.getImageUrl().isEmpty()){
                Glide.with(this).load(event.getImageUrl()).into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.poster);
            }
            eventName.setText(event.getEventName());


            eventCard.setOnClickListener(v -> openEventDetails(event));

            eventsContainer.addView(eventCard);
        }
    }

    /**
     * Filters the list of events based on the current button state.
     *
     * @param showActive
     * @see #displayEvents(ArrayList)
     */
    private void filterEvents(boolean showActive) {
        long currentTime = System.currentTimeMillis();
        ArrayList<Event> filtered = new ArrayList<>();

        // Reset both button UI states here (no helpers)
        currentButton.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.background_grey)));
        pastButton.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.background_grey)));

        if (showActive) {
            // Highlight active button
            currentButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.light_purple)));

            // Condition: Future events = Active
            for (Event event : userEvents) {
                long eventTime = Event.getEventTimestamp(event);
                if (eventTime > currentTime) {
                    filtered.add(event);
                }
            }
        } else {
            // Highlight closed button
            pastButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.light_purple)));

            // Condition: Already expired = Closed
            for (Event event : userEvents) {
                long eventTime = Event.getEventTimestamp(event);
                long eventEnd = eventTime + 24 * 60 * 60 * 1000; // 24 hours
                if (eventEnd < currentTime) {
                    filtered.add(event);
                }
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            displayEvents(filtered);
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
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }


    /**
     * Shows the empty state layout with a message and create event button.
     * Hides the events scroll view.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        eventsScrollView.setVisibility(View.GONE);
    }
}