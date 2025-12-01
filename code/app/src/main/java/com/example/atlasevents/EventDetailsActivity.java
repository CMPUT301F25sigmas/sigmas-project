package com.example.atlasevents;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.GeoPoint;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Date;

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
public class EventDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String MAP_VIEW_BUNDLE_KEY = "EventDetailsMapViewBundleKey";

    /**
     * Key used to pass the Event object through Intent extras.
     * This constant should be used when starting this activity to include
     * the event data in the intent.
     */
    public static final String EventKey = "com.example.atlasevents.EVENT";

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private FusedLocationProviderClient fusedLocationClient;
    private Session session;

    private Event currentEvent;
    private Entrant currentEntrant;
    private boolean pendingLocationPermissionForJoin;
    private GoogleMap eventMap;
    private LatLng eventLatLng;


    private TextView eventNameTextView, organizerNameTextView, descriptionTextView,
            waitlistCountTextView, dateTextView, timeTextView, locationTextView;
    private ImageView eventImageView, qrImageView, backArrow, guidelinesButton;
    private Button joinWaitlistButton, leaveWaitlistButton;
    private CheckBox optOutCheckBox;
    private MapView eventMapView;

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
        setContentView(R.layout.entrant_event_details);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventRepository = new EventRepository();
        userRepository = new UserRepository();
        session = new Session(this);

        guidelinesButton = findViewById(R.id.guidelinesButton);
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
        eventImageView = findViewById(R.id.eventImage);
        qrImageView = findViewById(R.id.qrImage);
        optOutCheckBox = findViewById(R.id.optOutCheckBox);
        eventMapView = findViewById(R.id.eventMapLocationView);

        initMap(savedInstanceState);
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
        userRepository.getEntrant(session.getUserEmail(), entrant -> {
            currentEntrant = entrant;
            tryUpdateWaitlistButtons();
        });
        String qrEventId = getIntent().getStringExtra("qrId");
        String tappedEventId = getIntent().getStringExtra(EventKey);
        if (qrEventId != null) {
            eventRepository.getEventById(qrEventId, new EventRepository.EventCallback() {
                @Override
                public void onSuccess(Event event) {
                    currentEvent = event;
                    displayEventDetails(event);
                    tryUpdateWaitlistButtons();
                    loadBlockedStatus();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("EventDetailsActivity", "Failed to fetch event", e);
                    Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else if (tappedEventId != null){
            //             eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            eventRepository.getEventById(tappedEventId, new EventRepository.EventCallback() {
                @Override
                public void onSuccess(Event event) {
                    currentEvent = event;
                    displayEventDetails(event);
                    tryUpdateWaitlistButtons();
                    loadBlockedStatus();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("EventDetailsActivity", "Failed to fetch event", e);
                    Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    /**
     * Ensures that the waitlist buttons are updated only after both
     * the current entrant and event data are loaded.
     */
    private void tryUpdateWaitlistButtons() {
        if (currentEntrant != null && currentEvent != null) {
            updateWaitlistButtons();
        }
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
        dateTextView.setText(event.getDateFormatted());
        timeTextView.setText(event.getTime());
        GeoPoint eventLocation = event.getLocation();
        if (eventLocation != null) {
            eventLatLng = new LatLng(eventLocation.getLatitude(), eventLocation.getLongitude());
            eventMapView.setVisibility(View.VISIBLE);
            renderEventLocation();
        } else {
            eventMapView.setVisibility(View.GONE);
        }

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
     * @version 1.2
     * Added listner for guidelines button to show lottery criteria
     */
    private void setupListeners() {
        backArrow.setOnClickListener(view -> finish());
        joinWaitlistButton.setOnClickListener(view -> joinWaitlist());
        leaveWaitlistButton.setOnClickListener(view -> leaveWaitlist());
        optOutCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentEvent != null && currentEvent.getOrganizer() != null) {
                updateBlockedStatus(isChecked);
            }
        });
        guidelinesButton.setOnClickListener(view -> showEventCriteriaDialog());
    }

    /**
     * Updates the visibility of the join and leave waitlist buttons based
     * on whether the current entrant is already on the waitlist.
     */
    private void updateWaitlistButtons() {
        if (currentEvent == null || currentEntrant == null) return;
        boolean inWaitlist = currentEvent.getWaitlist().containsEntrant(currentEntrant);
        boolean inAcceptedList = currentEvent.getAcceptedList().containsEntrant(currentEntrant);
        boolean inInvitedList = currentEvent.getInviteList().containsEntrant(currentEntrant);

        if (inAcceptedList){
            joinWaitlistButton.setVisibility(View.GONE);
            leaveWaitlistButton.setVisibility(View.GONE);
            findViewById(R.id.acceptedWaitlistButton).setVisibility(View.VISIBLE);
        }
        else if (inInvitedList){
            joinWaitlistButton.setVisibility(View.GONE);
            leaveWaitlistButton.setVisibility(View.GONE);
            findViewById(R.id.invitedWaitlistButton).setVisibility(View.VISIBLE);
        }
        else if (inWaitlist) {
            joinWaitlistButton.setVisibility(View.GONE);
            leaveWaitlistButton.setVisibility(View.VISIBLE);
        } else {
            joinWaitlistButton.setVisibility(View.VISIBLE);
            leaveWaitlistButton.setVisibility(View.GONE);
        }
    }

    /**
     * Attempts to add the current entrant to the event's waitlist.
     * <p>
     * Updates the event in Firestore and provides feedback to the user
     * through a Toast message.
     * </p>
     */
    private void joinWaitlist() {
        if (currentEvent == null || currentEntrant == null) return;

        if (currentEvent.getRequireGeolocation()
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingLocationPermissionForJoin = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        attemptJoinWaitlist();
    }

    private void attemptJoinWaitlist() {
        if (currentEvent == null || currentEntrant == null) return;
        pendingLocationPermissionForJoin = false;

        int joined = currentEvent.addToWaitlist(currentEntrant);
        if (joined == 1) {

            if (currentEvent.getRequireGeolocation()
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                                currentEvent.addToEntrantLocation(currentEntrant.getEmail(), geoPoint);
                            }
                            updateWaitList();

                        }).addOnFailureListener(e -> {
                            Log.e("EventDetails", "Location not found", e);
                            updateWaitList();
                        });
            } else {
                updateWaitList();
            }
        } else if (joined == 0) {
            Toast.makeText(this, "Waitlist limit reached", Toast.LENGTH_SHORT).show();
        } else if (joined == -1) {
            Toast.makeText(this, "Waitlist not open yet or past deadline", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWaitList() {
        eventRepository.updateEvent(currentEvent, success -> {
            if (success) {
                Toast.makeText(this, "Waitlist Joined Successfully", Toast.LENGTH_SHORT).show();
                waitlistCountTextView.setText(String.valueOf(currentEvent.getWaitlist().size()));
                updateWaitlistButtons();
            } else {
                Toast.makeText(this, "Failed to join Waitlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Removes the current entrant from the event's waitlist.
     * <p>
     * Updates the event in Firestore and refreshes the UI buttons
     * to reflect the updated waitlist status.
     * </p>
     */
    private void leaveWaitlist() {
        if (currentEvent == null || currentEntrant == null) return;

        currentEvent.removeFromWaitlist(currentEntrant);
        currentEvent.removeFromEntrantLocation(currentEntrant);
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
    
    /**
     * Loads the current blocked status for this organizer.
     * Checks if the organizer's email is in the user's blocked list.
     */
    private void loadBlockedStatus() {
        if (currentEvent == null || currentEvent.getOrganizer() == null) return;
        
        String userEmail = session.getUserEmail();
        String organizerEmail = currentEvent.getOrganizer().getEmail();
        
        userRepository.isOrganizerBlocked(userEmail, organizerEmail, isBlocked -> {
            optOutCheckBox.setOnCheckedChangeListener(null);
            optOutCheckBox.setChecked(isBlocked);
            optOutCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (currentEvent != null && currentEvent.getOrganizer() != null) {
                    updateBlockedStatus(isChecked);
                }
            });
        });
    }
    
    /**
     * Updates the blocked status for this organizer.
     * Adds or removes the organizer's email from the user's blocked list.
     * 
     * @param shouldBlock true to block notifications, false to unblock
     */
    private void updateBlockedStatus(boolean shouldBlock) {
        String userEmail = session.getUserEmail();
        String organizerEmail = currentEvent.getOrganizer().getEmail();
        
        if (shouldBlock) {
            userRepository.blockOrganizer(userEmail, organizerEmail, new UserRepository.BlockedOrganizersCallback() {
                @Override
                public void onResult(boolean isBlocked) {
                    Toast.makeText(EventDetailsActivity.this, 
                            "You will no longer receive notifications from this organizer", 
                            Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e("EventDetailsActivity", "Failed to block organizer", e);
                    Toast.makeText(EventDetailsActivity.this, 
                            "Failed to update preferences", 
                            Toast.LENGTH_SHORT).show();
                    optOutCheckBox.setChecked(false);
                }
            });
        } else {
            userRepository.unblockOrganizer(userEmail, organizerEmail, new UserRepository.BlockedOrganizersCallback() {
                @Override
                public void onResult(boolean isBlocked) {
                    Toast.makeText(EventDetailsActivity.this, 
                            "You will now receive notifications from this organizer", 
                            Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e("EventDetailsActivity", "Failed to unblock organizer", e);
                    Toast.makeText(EventDetailsActivity.this, 
                            "Failed to update preferences", 
                            Toast.LENGTH_SHORT).show();
                    optOutCheckBox.setChecked(true);
                }
            });
        }
    }

    private void initMap(Bundle savedInstanceState) {
        if (eventMapView == null) {
            return;
        }
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        eventMapView.onCreate(mapViewBundle);
        eventMapView.getMapAsync(this);
    }

    private void renderEventLocation() {
        if (eventMap == null || eventLatLng == null) {
            return;
        }
        eventMap.clear();
        eventMap.addMarker(new MarkerOptions().position(eventLatLng).title(currentEvent != null ? currentEvent.getEventName() : "Event Location"));
        eventMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 14f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        eventMap = googleMap;
        eventMap.getUiSettings().setZoomControlsEnabled(true);
        renderEventLocation();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (eventMapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
            }
            eventMapView.onSaveInstanceState(mapViewBundle);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (eventMapView != null) {
            eventMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventMapView != null) {
            eventMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (eventMapView != null) {
            eventMapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventMapView != null) {
            eventMapView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        if (eventMapView != null) {
            eventMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (eventMapView != null) {
            eventMapView.onLowMemory();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingLocationPermissionForJoin) {
                    attemptJoinWaitlist();
                }
            } else if (pendingLocationPermissionForJoin) {
                pendingLocationPermissionForJoin = false;
                Toast.makeText(this, "Location permission is required to join this event", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Shows a Material Design dialog with event criteria
     */
    private void showEventCriteriaDialog() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        int entrantLimit = currentEvent.getEntrantLimit();
        boolean requireGeolocation = currentEvent.getRequireGeolocation();
        Date registrationEnd = currentEvent.getRegEndDate();
        Date registrationStart = currentEvent.getRegStartDate();

        String entrantLimitText = entrantLimit == -1 ? "No limit" : String.valueOf(entrantLimit);
        String geolocationText = requireGeolocation ? "Yes" : "No";
        String registrationStartString = registrationStart.toString();
        String registrationEndString = registrationEnd.toString();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Event Criteria")
                .setMessage("Registration Start: "+ registrationStartString +"\n\nRegistration End: " + registrationEndString + "\n\nMaximum Entrants: " + entrantLimitText +
                        "\n\nGeolocation Required: " + geolocationText +
                        (requireGeolocation ?
                                "\n\nNote: This event requires location sharing to participate." :
                                ""))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Learn More", (dialog, which) -> {
                    // Optional: Show more information about what geolocation means
                    showGeolocationExplanation();
                })
                .show();
    }

    /**
     * Shows explanation about geolocation requirements
     */
    private void showGeolocationExplanation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About Geolocation")
                .setMessage("When geolocation is required for an event, it means:\n\n" +
                        "• Your location will be shared with the organizer during the event\n" +
                        "• This helps organizers manage event capacity and location\n" +
                        "• Your location data is only used for event purposes\n" +
                        "• You can control location permissions in your device settings")
                .setPositiveButton("Got it", null)
                .show();
    }
}
