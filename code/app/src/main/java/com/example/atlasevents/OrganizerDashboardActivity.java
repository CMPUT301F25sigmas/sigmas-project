package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OrganizerDashboardActivity extends AppCompatActivity {
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
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        String username = bundle.getString("email");

        Button createEventButton = findViewById(R.id.create_event_button);
        LinearLayout createEventTabButton = findViewById(R.id.create_event_icon);

        createEventTabButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            Bundle bundle2 = new Bundle();
            bundle2.putString("email",username); //using a bundle to pass user id to new activity
            intent.putExtras(bundle2);
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
}