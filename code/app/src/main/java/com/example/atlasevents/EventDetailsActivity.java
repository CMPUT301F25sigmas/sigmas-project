package com.example.atlasevents;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class EventDetailsActivity extends AppCompatActivity {

    public static final String EventKey = "com.example.atlasevents.EVENT";

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private Session session;

    private Event currentEvent;
    private Entrant currentEntrant;

    private TextView eventNameTextView, organizerNameTextView, descriptionTextView,
            waitlistCountTextView, dateTextView, timeTextView, locationTextView;
    private ImageView eventImageView, backArrow;
    private Button joinWaitlistButton, leaveWaitlistButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_event_details);

        eventRepository = new EventRepository();
        userRepository = new UserRepository();
        session = new Session(this);

        eventNameTextView = findViewById(R.id.eventName);
        organizerNameTextView = findViewById(R.id.organizerName);
        descriptionTextView = findViewById(R.id.eventDescription);
        waitlistCountTextView = findViewById(R.id.waitlistCount);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        locationTextView = findViewById(R.id.locationTextView);
        eventImageView = findViewById(R.id.eventImage);
        backArrow = findViewById(R.id.back_arrow);
        joinWaitlistButton = findViewById(R.id.joinWaitlistButton);
        leaveWaitlistButton = findViewById(R.id.leaveWaitlistButton);

        loadData();
        setupListeners();
    }

    private void loadData(){
        userRepository.getEntrant(session.getUserEmail(), entrant -> {
            currentEntrant = entrant;
            tryUpdateWaitlistButtons();
        });
        eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
                tryUpdateWaitlistButtons();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EventDetailsActivity", "Failed to fetch event", e);
                Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void tryUpdateWaitlistButtons() {
        if (currentEntrant != null && currentEvent != null) {
            updateWaitlistButtons();
        }
    }

    private void displayEventDetails(Event event) {
        eventNameTextView.setText(event.getEventName());
        organizerNameTextView.setText(event.getOrganizer().getName());
        descriptionTextView.setText(event.getDescription());
        locationTextView.setText(event.getAddress());
        dateTextView.setText(event.getDate());
        timeTextView.setText(event.getTime());

        waitlistCountTextView.setText(String.valueOf(
                event.getWaitlist() != null ? event.getWaitlist().size() : 0));

//        eventImageView.setImageBitmap(generateQRCode(event.getId()));
    }

    private Bitmap generateQRCode(String eventId) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, 300, 300);
            Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565);

            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (Exception e) {
            Log.e("EventDetailsActivity", "Error generating QR code", e);
            return null;
        }
    }

    private void setupListeners() {
        backArrow.setOnClickListener(view -> finish());
        joinWaitlistButton.setOnClickListener(view -> joinWaitlist());
        leaveWaitlistButton.setOnClickListener(view -> leaveWaitlist());
    }

    private void updateWaitlistButtons() {
        if (currentEvent == null || currentEntrant == null) return;
        boolean inWaitlist = currentEvent.getWaitlist().containsEntrant(currentEntrant);

        if (inWaitlist) {
            joinWaitlistButton.setVisibility(View.GONE);
            leaveWaitlistButton.setVisibility(View.VISIBLE);
        } else {
            joinWaitlistButton.setVisibility(View.VISIBLE);
            leaveWaitlistButton.setVisibility(View.GONE);
        }
    }

    private void joinWaitlist() {
        if (currentEvent == null || currentEntrant == null) return;

        int joined = currentEvent.addToWaitlist(currentEntrant);
        if (joined == 1) {
            eventRepository.updateEvent(currentEvent, success -> {
                if (success) {
                    Toast.makeText(this, "Joined waitlist successfully", Toast.LENGTH_SHORT).show();
                    waitlistCountTextView.setText(String.valueOf(currentEvent.getWaitlist().size()));
                    updateWaitlistButtons();
                } else {
                    Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (joined == 0) {
            Toast.makeText(this, "Waitlist limit reached", Toast.LENGTH_SHORT).show();
        }
    }

    private void leaveWaitlist() {
        if (currentEvent == null || currentEntrant == null) return;

        currentEvent.removeFromWaitlist(currentEntrant);
        eventRepository.updateEvent(currentEvent, success -> {
            if (success) {
                Toast.makeText(this, "Left waitlist successfully", Toast.LENGTH_SHORT).show();
                waitlistCountTextView.setText(String.valueOf(currentEvent.getWaitlist().size()));
                updateWaitlistButtons();
            } else {
                Toast.makeText(this, "Failed to leave waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
