package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.utils.NotificationManager;
import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.DebugNotificationActivity;

public class OrganizerDashboardActivity extends AppCompatActivity {
    private Session session;

    /***
     * listener for notifications added to organiser dashboard as this is anopther foreground/ main activity
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
    to do:  -allow organizer to make a new Event

     */

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.organizer_dashboard_empty);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Organizer user;
        session = new Session(this);
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        String username = bundle.getString("email");

        Button createEventButton = findViewById(R.id.create_event_button);
        // inside onCreate() after setContentView(...)
        Button debugButton = findViewById(R.id.notification_debug_button);
        debugButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, DebugNotificationActivity.class);
            startActivity(intent);
        });


        createEventButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            Bundle bundle2 = new Bundle();
            bundle2.putString("email",username); //using a bundle to pass user id to new activity
            intent.putExtras(bundle2);
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

    NotificationRepository notifRepo = new NotificationRepository();// 1) When organizer runs lottery and moves users to inviteList -> notify chosen entrants
        public void notifyChosenEntrants(Event event) {
            String title = "You've been selected!";
            String message = "You are invited to sign up for " + event.getEventName() + ". Please confirm your spot.";
            notifRepo.sendToInvited(event, title, message)
                    .addOnFailureListener(e -> Log.e("Organizer", "Failed sending to invited", e));
        }

// 2) Send to waitlist (US 02.07.01)
        public void notifyWaitlist(Event event, String customMessage) {
            String title = "Update about " + event.getEventName();
            notifRepo.sendToWaitlist(event, title, customMessage)
                    .addOnFailureListener(e -> Log.e("Organizer", "Failed sending to waitlist", e));
        }

// 3) Send to all selected (US 02.07.02) — same as invited
// 4) Send to cancelled entrants (US 02.07.03)
        public void notifyCancelled(Event event) {
            String title = "Event Cancelled: " + event.getEventName();
            String message = "This event has been cancelled. Please check your dashboard for details.";
            notifRepo.sendToCancelled(event, title, message)
                    .addOnFailureListener(e -> Log.e("Organizer", "Failed sending to cancelled list", e));
        }
}