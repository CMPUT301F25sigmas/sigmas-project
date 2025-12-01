package com.example.atlasevents;

import android.app.Activity;
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

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.utils.MapWarmUpManager;
import com.example.atlasevents.utils.NotificationManager;
import com.example.atlasevents.data.UserRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


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
/**
 * Main dashboard activity for event organizers.
 * Provides navigation to event creation, notification center, and serves as the
 * primary hub for organizer operations. Manages notification listening during
 * the activity's foreground state.
 *
 * <p>Extends OrganizerBase to inherit common organizer functionality and
 * integrates with NotificationManager for real-time notifications.</p>
 *
 * @see OrganizerBase
 * @see NotificationManager
 * @see Session
 */
public class OrganizerDashboardActivity extends OrganizerBase {
    private Session session;

    /**
     * Initializes the organizer dashboard and sets up UI components.
     * Configures notification icon click listener and event creation button.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down then this Bundle contains the data it
     *                           most recently supplied in onSaveInstanceState(Bundle)
     * @see #onStart()
     * @see #onStop()
     * @see NotificationCenterActivity
     * @see CreateEventActivity
     */


    /**
     * Repository for accessing and managing event data from Firestore.
     */
    EventRepository eventRepository;

    /**
     * Container layout that holds individual event card views.
     */
    LinearLayout eventsContainer;

    /**
     * Scrollable view containing the events container.
     */
    ScrollView eventsScrollView;

    /**
     * Layout displayed when the organizer has no events.
     */
    LinearLayout emptyState;

    private ArrayList<Event> allEvents = new ArrayList<>();
    private Button allButton, activeButton, ongoingButton, closedButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_dashboard_empty);
        setActiveNavItem(R.id.events_icon_card);
        MapWarmUpManager.warmUp(getApplicationContext());
        session = new Session(this);

        // Set up notification icon click listener
        findViewById(R.id.notifications_icon).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationCenterActivity.class);
            startActivity(intent);
        });

        allButton = findViewById(R.id.FilterAllButton);
        activeButton = findViewById(R.id.filterActivebutton);
        ongoingButton = findViewById(R.id.filterOngoingButton);
        closedButton = findViewById(R.id.filterClosedButton);

        allButton.setOnClickListener(v -> filterEvents(FilterType.ALL));
        activeButton.setOnClickListener(v -> filterEvents(FilterType.ACTIVE));
        ongoingButton.setOnClickListener(v -> filterEvents(FilterType.ONGOING));
        closedButton.setOnClickListener(v -> filterEvents(FilterType.CLOSED));

        eventsContainer = findViewById(R.id.events_container_organizer);
        Button createEventButton = findViewById(R.id.create_event_button);
        // inside onCreate() after setContentView(...)
        //Button debugButton = findViewById(R.id.notification_debug_button);
        //debugButton.setOnClickListener(v -> {
            //Intent intent = new Intent(OrganizerDashboardActivity.this, DebugNotificationActivity.class);
            //startActivity(intent);
        //});


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
     * Called when the activity becomes visible to the user.
     * Starts listening for notifications for the current organizer.
     *
     * @see #onStop()
     * @see NotificationManager#startListening(Activity, String)
     */
    @Override
    protected void onStart() {
        super.onStart();
        NotificationManager.startListening(this, session.getUserEmail());
    }
    /**
     * Called when the activity is no longer visible to the user.
     * Stops notification listening to conserve resources.
     *
     * @see #onStart()
     * @see NotificationManager#stopListening()
     */
    @Override
    protected void onStop() {
        super.onStop();
        NotificationManager.stopListening();
    }

    /**
     * Loads all events created by the currently logged-in organizer from Firestore.
     * Retrieves the organizer information using the session email, then queries for their events.
     * Displays events if found, otherwise shows the empty state.
     */
    void loadOrganizerEvents() {
        userRepository.getOrganizer(session.getUserEmail(), organizer -> {
            if (organizer != null) {
                eventRepository.getEventsByOrganizer(organizer.getEmail(), new EventRepository.EventsCallback() {
                    @Override
                    public void onSuccess(ArrayList<Event> events) {
                        if (events.isEmpty()) {
                            showEmptyState();
                        } else {
                            allEvents.clear();
                            allEvents.addAll(events);
                            displayEvents(allEvents);

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
    void displayEvents(ArrayList<Event> events) {
        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);

        eventsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View eventCard = inflater.inflate(R.layout.organizer_event_cards, eventsContainer, false);

            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            TextView eventName = eventCard.findViewById(R.id.event_name);
            ImageView eventEditButton = eventCard.findViewById(R.id.edit_button);

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

    enum FilterType {
        ALL, ACTIVE, ONGOING, CLOSED
    }


    void filterEvents(FilterType filterType) {
        ArrayList<Event> filtered = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Reset all buttons first
        resetButtonStates();
        
        // Set the active button color
        switch (filterType) {
            case ALL:
                allButton.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.light_purple)));
                filtered = new ArrayList<>(allEvents);
                break;

            case ACTIVE:
                // Active = event registration is open and event hasn't started yet
                activeButton.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.light_purple)));
                for (Event event : allEvents) {
                    if (event.getDate() != null && event.getDate().getTime() > currentTime) {
                        filtered.add(event);
                    }
                }
                break;

            case ONGOING:
                // Ongoing = event has started but not yet ended (using event date as the day of the event)
                ongoingButton.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.light_purple)));
                for (Event event : allEvents) {
                    if (event.getDate() != null) {
                        long eventTime = event.getDate().getTime();
                        // Consider an event as ongoing for the entire day
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(event.getDate());
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        long endOfDay = cal.getTimeInMillis();
                        
                        if (eventTime <= currentTime && currentTime <= endOfDay) {
                            filtered.add(event);
                        }
                    }
                }
                break;

            case CLOSED:
                // Closed = event has passed (after the event day)
                closedButton.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.light_purple)));
                for (Event event : allEvents) {
                    if (event.getDate() != null) {
                        // Consider event as closed if it's past the event day
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(event.getDate());
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        long endOfDay = cal.getTimeInMillis();
                        
                        if (endOfDay < currentTime) {
                            filtered.add(event);
                        }
                    }
                }
                break;
        }

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            displayEvents(filtered);
        }
    }

    private void resetButtonStates() {
        int defaultColor = ContextCompat.getColor(this, R.color.background_grey);
        allButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        activeButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        ongoingButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        closedButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
    }


    /**
     * Shows the empty state layout with a message and create event button.
     * Hides the events scroll view.
     */
    void showEmptyState() {
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
