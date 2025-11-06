package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.utils.NotificationManager;

public class OrganizerDashboardActivity extends OrganizerBase {
    private Session session;

    /***
     * listener for notifications added to organiser dashboard as this is anopther foreground/ main activity
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_dashboard_empty);
        session = new Session(this);

        // Set up notification icon click listener
        findViewById(R.id.notifications_icon).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationCenterActivity.class);
            startActivity(intent);
        });

        Button createEventButton = findViewById(R.id.create_event_button);
        // inside onCreate() after setContentView(...)
        Button debugButton = findViewById(R.id.notification_debug_button);
        debugButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, DebugNotificationActivity.class);
            startActivity(intent);
        });


        createEventButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            startActivity(intent);
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        NotificationManager.startListening(this, session.getUserEmail());
    }

    @Override
    protected void onStop() {
        super.onStop();
        NotificationManager.stopListening();
    }
}
