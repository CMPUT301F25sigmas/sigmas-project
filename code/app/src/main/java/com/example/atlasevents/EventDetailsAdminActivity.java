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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Activity for displaying detailed information about an event.
 * <p>
 * This activity shows event details including the event name, organizer,
 * description, and a QR code representation of the event ID. It also provides
 * functionality for entrants to join or leave the event waitlist.
 * </p>
 * <p>
 * The event object is passed to this activity via an Intent extra using the
 * {@link #EventKey} identifier.
 * </p>
 *
 * @see Event
 */
public class EventDetailsAdminActivity extends AppCompatActivity {

    /**
     * Key used to pass the Event object through Intent extras.
     * This constant should be used when starting this activity to include
     * the event data in the intent.
     */
    public static final String EventKey = "com.example.atlasevents.EVENT";

    private EventRepository eventRepository;
    private Event currentEvent;
    private Entrant currentEntrant;

    private TextView eventNameTextView, organizerNameTextView, descriptionTextView,
            waitlistCountTextView, dateTextView, timeTextView, locationTextView;
    private ImageView eventImageView, qrImageView, backArrow, deleteButton;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components and populates them with event data retrieved
     * from the Intent extras. Sets up click listeners for the join and leave
     * waitlist buttons (functionality to be implemented).
     * </p>
     * <p>
     * The event object is retrieved using {@link #EventKey} and its details are
     * displayed including:
     * </p>
     * <ul>
     *   <li>Event name</li>
     *   <li>Organizer name</li>
     *   <li>Event description</li>
     *   <li>QR code generated from the event ID</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_event_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventRepository = new EventRepository();

        eventNameTextView = findViewById(R.id.eventName);
        organizerNameTextView = findViewById(R.id.organizerName);
        descriptionTextView = findViewById(R.id.eventDescription);
        waitlistCountTextView = findViewById(R.id.waitlistCount);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        locationTextView = findViewById(R.id.locationTextView);
        eventImageView = findViewById(R.id.eventImage);
        backArrow = findViewById(R.id.back_arrow);
        eventImageView = findViewById(R.id.eventImage);
        qrImageView = findViewById(R.id.qrImage);

        deleteButton = findViewById(R.id.delete_icon);

        loadData();
        setupListeners();
    }

    /**
     * Loads the event and entrant data from the repositories.
     * <p>
     * Fetches the currently logged-in entrant based on the stored session email
     * and retrieves the selected event from Firestore using its ID.
     * </p>
     */
    private void loadData(){
        eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EventDetailsActivity", "Failed to fetch event", e);
                Toast.makeText(EventDetailsAdminActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Displays event details on the screen.
     * <p>
     * Populates all text fields, loads the event image using Glide,
     * and generates a QR code for the event ID.
     * </p>
     *
     * @param event The {@link Event} object containing event information.
     */
    private void displayEventDetails(Event event) {
        eventNameTextView.setText(event.getEventName());
        organizerNameTextView.setText(event.getOrganizer().getName());
        descriptionTextView.setText(event.getDescription());
        locationTextView.setText(event.getAddress());
        dateTextView.setText(event.getDate());
        timeTextView.setText(event.getTime());

        waitlistCountTextView.setText(String.valueOf(
                event.getWaitlist() != null ? event.getWaitlist().size() : 0));

        if(!event.getImageUrl().isEmpty()){
            Glide.with(this).load(event.getImageUrl()).into(eventImageView);
        } else {
            eventImageView.setImageResource(R.drawable.poster);
        }
        eventImageView.setVisibility(View.VISIBLE);

        qrImageView.setImageBitmap(generateQRCode(event.getId()));
        qrImageView.setVisibility(View.VISIBLE);
    }

    /**
     * Generates a QR code bitmap from the given event ID.
     * <p>
     * Creates a 300x300 pixel QR code image using the ZXing library.
     * The QR code encodes the event ID as a string and renders it in
     * black and white.
     * </p>
     *
     * @param eventId The unique identifier of the event to encode in the QR code
     * @return A Bitmap containing the generated QR code image
     * @throws RuntimeException if QR code generation fails
     */
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

    /**
     * Sets up click listeners for UI interactions such as the back arrow
     * and the waitlist join/leave buttons.
     */
    private void setupListeners() {
        backArrow.setOnClickListener(view -> finish());
        deleteButton.setOnClickListener(view -> {
            eventRepository.deleteEvent(currentEvent.getId());
            finish();
            Toast.makeText(this, "Event deleted succesfully.", Toast.LENGTH_SHORT).show();
        });
    }
}
