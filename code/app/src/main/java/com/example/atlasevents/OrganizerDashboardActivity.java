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

public class OrganizerDashboardActivity extends OrganizerBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_dashboard_empty);
        Button createEventButton = findViewById(R.id.create_event_button);
        createEventButton.setOnClickListener(view -> {
            Intent intent = new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });
    }
}