package com.example.atlasevents;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Activity displaying the admin's dashboard with a list of available events.
 * <p>
 * This activity extends {@link AdminBase} to provide the navigation sidebar and
 * displays all events retrieved from Firebase. Events are shown as cards that admins
 * can tap to view detailed information. The activity handles fetching events from
 * the repository and dynamically creating event card views.
 * </p>
 *
 * @see AdminBase
 * @see Event
 * @see EventRepository
 * @see EventDetailsActivity
 */
public class AdminDashboardActivity extends AdminBase {

    /**
     * Container layout that holds all event card views.
     */
    private LinearLayout eventsContainer;

    /**
     * Repository for fetching event data from Firebase.
     */
    private EventRepository eventRepository;

    /**
     * Scroll view containing the list of events.
     */
    private ScrollView eventsScrollView;

    /**
     * Layout for displaying a message when no events are available.
     */
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.admin_dashboard);
        setActiveNavItem(R.id.events_icon_card);

        eventsContainer = findViewById(R.id.events_container_organizer);
        eventRepository = new EventRepository();

        eventsScrollView = findViewById(R.id.events_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.GONE);

        loadEventsFromFirebase();
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
        eventRepository.getAllEvents(new EventRepository.EventsCallback(){
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
     * Displays a list of events as card views in the events container.
     * <p>
     * Clears any existing event cards and dynamically inflates new card views
     * for each event in the list. Each card shows the event name and image,
     * and is clickable to open event details.
     * </p>
     *
     * @param events The list of events to display
     */
    private void displayEvents(ArrayList<Event> events) {
        emptyState.setVisibility(View.GONE);
        eventsScrollView.setVisibility(View.VISIBLE);
        eventsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View eventCard = inflater.inflate(R.layout.event_card_admin_item, eventsContainer, false);

            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            ImageView menuButton = eventCard.findViewById(R.id.menu_button);
            TextView eventName = eventCard.findViewById(R.id.event_name);

            if (!event.getImageUrl().isEmpty()) {
                Glide.with(this).load(event.getImageUrl()).into(eventImage);
            } else {
                eventImage.setImageResource(R.drawable.poster);
            }
            eventName.setText(event.getEventName());

            menuButton.setOnClickListener(v -> {
                View dropdownView = inflater.inflate(R.layout.event_dropdown, null);

                PopupWindow popupWindow = new PopupWindow(dropdownView, eventCard.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);

                popupWindow.setOutsideTouchable(true);

                popupWindow.showAsDropDown(eventCard, 0, -eventCard.getHeight()+150);

                dropdownView.findViewById(R.id.action_view_details).setOnClickListener(item -> {
                    openEventDetails(event);
                    popupWindow.dismiss();
                });

                dropdownView.findViewById(R.id.action_remove_event).setOnClickListener(item -> {
                    eventRepository.deleteEvent(event.getId());
                    loadEventsFromFirebase();
                    popupWindow.dismiss();
                });
            });

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
        Intent intent = new Intent(this, EventDetailsAdminActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsFromFirebase();
    }

    /**
     * Shows the empty state layout with a message and hides the events scroll view.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        eventsScrollView.setVisibility(View.GONE);
    }
}