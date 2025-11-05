package com.example.atlasevents;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class EventDetailsActivity extends AppCompatActivity {
    public static String EventKey = "com.example.atlasevents.EVENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_event_details);
        Event event = (Event) getIntent().getSerializableExtra(EventKey);
        TextView eventNameTextView = findViewById(R.id.eventName);
        TextView organizerNameTextView = findViewById(R.id.organizerName);
        TextView descriptionTextView = findViewById(R.id.eventDescription);

        if (event != null) {
            eventNameTextView.setText(event.getEventName());
            organizerNameTextView.setText(event.getOrganizer().getName());
            descriptionTextView.setText(event.getDescription());
        }

    }
}
