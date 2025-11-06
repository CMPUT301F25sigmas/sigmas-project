package com.example.atlasevents;

import android.content.ContentResolver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

/**
 * Activity for editing existing event details.
 * <p>
 * This activity allows organizers to modify all aspects of an event including name,
 * dates, times, location, description, participant limits, geolocation requirements,
 * and event poster images. It handles image uploads, deletions, and validates inputs
 * before saving changes to the repository.
 * </p>
 * <p>
 * The activity manages complex image lifecycle including uploading new images,
 * deleting old images, and cleaning up unsaved images when the activity is closed.
 * </p>
 *
 * @see Event
 * @see EventRepository
 * @see ImageUploader
 */
public class EditEventActivity extends AppCompatActivity {
    /**
     * Intent extra key for passing the event to be edited.
     */
    public static final String EventKey = "com.example.atlasevents.EVENT";

    /**
     * Repository for event data operations.
     */
    EventRepository eventRepo = new EventRepository();

    /**
     * Current user session containing authentication information.
     */
    Session session;

    /**
     * Flag indicating whether the event was successfully saved.
     * Used to determine image cleanup behavior on activity exit.
     */
    private boolean eventSaved = false;

    /**
     * Flag indicating whether the original event image should be deleted.
     */
    private boolean deleteOldImage = false;

    /**
     * Utility for uploading and deleting images from Firebase Storage.
     */
    ImageUploader uploader;

    /**
     * The event object being edited.
     */
    private Event currentEvent;

    /**
     * EditText field for the event name.
     */
    private EditText nameEditText;

    /**
     * EditText field for the event date.
     */
    private EditText dateEditText;

    /**
     * EditText field for the event time.
     */
    private EditText timeEditText;

    /**
     * EditText field for the registration start date.
     */
    private EditText regStartDateEditText;

    /**
     * EditText field for the registration end date.
     */
    private EditText regEndDateEditText;

    /**
     * EditText field for the event description.
     */
    private EditText descriptionEditText;

    /**
     * EditText field for the event location.
     */
    private EditText locationEditText;

    /**
     * Switch for enabling or disabling entrant limits.
     */
    private SwitchCompat limitEntrantsSwitch;

    /**
     * Switch for requiring geolocation from entrants.
     */
    private SwitchCompat requireGeoLocationSwitch;

    /**
     * EditText field for the maximum number of entrants.
     */
    private EditText entrantLimitEditText;

    /**
     * EditText field for the number of available slots.
     */
    private EditText slotsEditText;

    /**
     * Button to save and update the event.
     */
    private Button updateButton;

    /**
     * Button to delete the event poster image.
     */
    private Button imageDeleteButton;

    /**
     * Launcher for the image picker activity.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    /**
     * URL of the newly uploaded image.
     * Empty string if no new image has been uploaded.
     */
    private String imageURL = "";

    /**
     * URL of the original event image.
     * Empty string if the event had no image.
     */
    private String oldImageURL = "";

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components, sets up the image picker, configures all
     * input fields and switches, loads the existing event data, and sets up
     * click listeners for update, upload, and delete operations.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied in onSaveInstanceState.
     *                           Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.event_creation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton backButton = findViewById(R.id.createBackButton);
        backButton.setOnClickListener(view -> finish());

        session = new Session(this);

        nameEditText = findViewById(R.id.nameEditText);
        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        regStartDateEditText = findViewById(R.id.startDateEditText);
        regEndDateEditText = findViewById(R.id.endDateEditText);
        descriptionEditText = findViewById(R.id.descrEditText);
        locationEditText = findViewById(R.id.locEditText);
        limitEntrantsSwitch = findViewById(R.id.limitEntrantsSwitch);
        requireGeoLocationSwitch = findViewById(R.id.requireGeoLocationSwitch);
        entrantLimitEditText = findViewById(R.id.maxEntrantsEditText);
        slotsEditText = findViewById(R.id.slotsEditText);
        updateButton = findViewById(R.id.publishEventButton);
        updateButton.setText("Update Event");

        entrantLimitEditText.setVisibility(View.GONE);
        limitEntrantsSwitch.setOnClickListener(view -> {
            if (limitEntrantsSwitch.isChecked()) {
                entrantLimitEditText.setVisibility(View.VISIBLE);
            } else {
                entrantLimitEditText.setVisibility(View.GONE);
            }
        });

        updateButton.setOnClickListener(view -> updateEvent());

        uploader = new ImageUploader();

        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        uploader.uploadImage(getContentResolver(), uri, new ImageUploader.UploadCallback() {
                            @Override
                            public void onSuccess(String url) {
                                if(!imageURL.isEmpty()){
                                    uploader.deleteImage(imageURL, new ImageUploader.DeleteCallback() {
                                        @Override
                                        public void onSuccess() {}
                                        @Override
                                        public void onFailure(String error) {}
                                    });
                                }
                                imageURL = url;
                                imageDeleteButton.setVisibility(View.VISIBLE);
                                loadImage(imageURL);
                                Toast.makeText(EditEventActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(EditEventActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        Button imageUploadButton = findViewById(R.id.uploadPosterButton);
        imageDeleteButton = findViewById(R.id.removePosterButton);
        imageUploadButton.setOnClickListener(v -> pickMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));
        imageDeleteButton.setOnClickListener(v ->{
            if(!imageURL.isEmpty()){
                uploader.deleteImage(imageURL, new ImageUploader.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        v.setVisibility(View.GONE);
                        imageURL="";
                        ImageView poster = findViewById(R.id.posterImageView);
                        poster.setImageResource(R.drawable.poster);
                    }
                    @Override
                    public void onFailure(String error) {
                    }
                });
            } else if (!oldImageURL.isEmpty()) {
                v.setVisibility(View.GONE);
                deleteOldImage = true;
                ImageView poster = findViewById(R.id.posterImageView);
                poster.setImageResource(R.drawable.poster);
            }
        });

        loadEventData();
    }

    /**
     * Loads the existing event data from the repository.
     * <p>
     * Fetches the event by ID from the intent extras and populates all UI fields
     * with the current event values. If the event has an associated image, it is
     * loaded and displayed. On failure, an error message is shown and the activity
     * is closed.
     * </p>
     */
    private void loadEventData() {
        eventRepo.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                nameEditText.setText(event.getEventName());
                dateEditText.setText(event.getDate());
                timeEditText.setText(event.getTime());
                regStartDateEditText.setText(event.getRegStartDate());
                regEndDateEditText.setText(event.getRegEndDate());
                descriptionEditText.setText(event.getDescription());
                locationEditText.setText(event.getAddress());
                slotsEditText.setText(String.valueOf(event.getSlots()));
                requireGeoLocationSwitch.setChecked(event.getRequireGeolocation());
                if (event.getEntrantLimit() > 0) {
                    limitEntrantsSwitch.setChecked(true);
                    entrantLimitEditText.setVisibility(View.VISIBLE);
                    entrantLimitEditText.setText(String.valueOf(event.getEntrantLimit()));
                } else {
                    limitEntrantsSwitch.setChecked(false);
                    entrantLimitEditText.setVisibility(View.GONE);
                }

                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    oldImageURL = event.getImageUrl();
                    loadImage(oldImageURL);
                    imageDeleteButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EditEventActivity", "Failed to fetch event", e);
                Toast.makeText(EditEventActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Updates the event with modified values from the UI fields.
     * <p>
     * Validates inputs, updates all event properties with the current field values,
     * handles image URL updates and deletions, and saves the changes to the repository.
     * Manages cleanup of old images when a new image replaces them. Displays success
     * or failure messages and closes the activity on success.
     * </p>
     */
    private void updateEvent() {
        if (!inputsValid(nameEditText.getText().toString(), slotsEditText.getText().toString())) {
            return;
        }

        currentEvent.setEventName(nameEditText.getText().toString());
        currentEvent.setDate(dateEditText.getText().toString());
        currentEvent.setTime(timeEditText.getText().toString());
        currentEvent.setRegStartDate(regStartDateEditText.getText().toString());
        currentEvent.setRegEndDate(regEndDateEditText.getText().toString());
        currentEvent.setAddress(locationEditText.getText().toString());
        currentEvent.setDescription(descriptionEditText.getText().toString());
        currentEvent.setRequireGeolocation(requireGeoLocationSwitch.isChecked());
        currentEvent.setSlots(Integer.parseInt(slotsEditText.getText().toString()));

        if (limitEntrantsSwitch.isChecked()) {
            String limit = entrantLimitEditText.getText().toString();
            currentEvent.setEntrantLimit(Integer.parseInt(limit));
        } else {
            currentEvent.setEntrantLimit(-1);
        }

        if (!imageURL.isEmpty()) { // Add image URL if uploaded
            currentEvent.setImageUrl(imageURL);
        }
        if (deleteOldImage) {
            currentEvent.setImageUrl("");
        }

        eventRepo.updateEvent(currentEvent, success -> {
            if (success) {
                eventSaved = true;
                if(!imageURL.isEmpty() && !oldImageURL.isEmpty()){
                    uploader.deleteImage(oldImageURL, new ImageUploader.DeleteCallback() {
                        @Override
                        public void onSuccess() {}
                        @Override
                        public void onFailure(String error) {}
                    });
                }
                Toast.makeText(EditEventActivity.this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditEventActivity.this, "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads and displays an image from the specified URL.
     * <p>
     * Uses Glide library to load the image from the URL and display it in the
     * poster ImageView.
     * </p>
     *
     * @param imageURL The URL of the image to load and display
     */
    public void loadImage(String imageURL) {
        ImageView poster = findViewById(R.id.posterImageView);
        Glide.with(this).load(imageURL).into(poster);
    }

    public boolean inputsValid(String name, String slots) {
        boolean valid = true;
        if (name.isEmpty()) {
            Toast.makeText(this, "Event must have name", Toast.LENGTH_SHORT).show();
            valid = false;
        } else if (slots.isEmpty()) {
            Toast.makeText(this, "Number of participants can not be empty", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        return valid;
    }

    /**
     * Called when the activity is finishing.
     * <p>
     * Handles cleanup of uploaded images based on whether the event was saved.
     * If a new image was uploaded but the event was not saved, the new image is deleted.
     * If the event was saved with a new image, the old image is deleted. This prevents
     * orphaned images in Firebase Storage.
     * </p>
     */
    @Override
    public void finish() {
        if (!imageURL.isEmpty() && !eventSaved) {
            uploader.deleteImage(imageURL, new ImageUploader.DeleteCallback() {
                @Override
                public void onSuccess() {
                    imageURL = "";
                    EditEventActivity.super.finish();
                }
                @Override
                public void onFailure(String error) {
                    Log.e("CreateEventActivity", "Failed to delete image: " + error);
                    EditEventActivity.super.finish();
                }
            });
        } else if(!oldImageURL.isEmpty() && eventSaved){
            uploader.deleteImage(oldImageURL, new ImageUploader.DeleteCallback() {
                @Override
                public void onSuccess() {
                    oldImageURL = "";
                    EditEventActivity.super.finish();
                }
                @Override
                public void onFailure(String error) {
                    Log.e("CreateEventActivity", "Failed to delete image: " + error);
                    EditEventActivity.super.finish();
                }
            });
        }else {
            super.finish();
        }
    }
}
