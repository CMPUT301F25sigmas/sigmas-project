package com.example.atlasevents;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.utils.NotificationManager;
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
        //Button debugButton = findViewById(R.id.notification_debug_button);
        //debugButton.setOnClickListener(v -> {
            //Intent intent = new Intent(OrganizerDashboardActivity.this, DebugNotificationActivity.class);
            //startActivity(intent);
        //});


        createEventButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            startActivity(intent);
                });

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
}
