package com.example.atlasevents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.NotificationListener;

import java.util.ArrayList;

public class EntrantDashboardActivity extends AppCompatActivity {

    private LinearLayout eventsContainer;
    private EventRepository eventRepository;

    private NotificationListener notificationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventsContainer = findViewById(R.id.events_container);
        eventRepository = new EventRepository();
        notificationListener = new NotificationListener(this);

        loadEventsFromFirebase();

    }

    /***
     * listener for notifications added to event dashboard as this is the foreground/ main activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (notificationListener != null) notificationListener.start();
    }

    @Override
    protected void onPause() {
        if (notificationListener != null) notificationListener.stop();
        super.onPause();
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