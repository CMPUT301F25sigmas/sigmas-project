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

public class EditEventActivity extends AppCompatActivity {
    public static final String EventKey = "com.example.atlasevents.EVENT";
    EventRepository eventRepo = new EventRepository();
    Session session;
    private boolean eventSaved = false;
    private boolean deleteOldImage = false;
    ImageUploader uploader;
    private Event currentEvent;
    private EditText nameEditText;
    private EditText dateEditText;
    private EditText timeEditText;
    private EditText regStartDateEditText;
    private EditText regEndDateEditText;
    private EditText descriptionEditText;
    private EditText locationEditText;
    private SwitchCompat limitEntrantsSwitch;
    private SwitchCompat requireGeoLocationSwitch;
    private EditText entrantLimitEditText;
    private EditText slotsEditText;
    private Button updateButton;
    private Button imageDeleteButton;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private String imageURL = "";

    private String oldImageURL = "";

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
