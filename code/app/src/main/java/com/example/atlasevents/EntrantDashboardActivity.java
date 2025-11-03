package com.example.atlasevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

public class EntrantDashboardActivity extends EntrantBase {

    private LinearLayout eventsContainer;
    private EventRepository eventRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_dashboard);

        eventsContainer = findViewById(R.id.events_container);
        eventRepository = new EventRepository();

        loadEventsFromFirebase();
    }

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

    private void openEventDetails(Event event) {
        // TODO: Navigate to event details page
    }
}