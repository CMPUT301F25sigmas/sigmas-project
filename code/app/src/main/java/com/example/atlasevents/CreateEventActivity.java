package com.example.atlasevents;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
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
import com.example.atlasevents.data.UserRepository;

public class CreateEventActivity extends AppCompatActivity {
    UserRepository userRepo = new UserRepository();
    EventRepository eventRepo = new EventRepository();
    Session session;
    private boolean eventSaved = false;

    ImageUploader uploader;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private String imageURL = "";

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     *     todo: make "limit number of entrants" just a editText instead of a switch and editText
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

        uploader = new ImageUploader();

        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        uploader = new ImageUploader();
                        uploader.uploadImage(uri, new ImageUploader.UploadCallback() {
                            @Override
                            public void onSuccess(String url) {
                                if(!imageURL.isEmpty()){
                                    uploader.deleteImage(imageURL, new ImageUploader.DeleteCallback() {
                                        @Override
                                        public void onSuccess() {
                                        }
                                        @Override
                                        public void onFailure(String error) {
                                        }
                                    });
                                }
                                imageURL = url;
                                loadImage();
                                findViewById(R.id.removePosterButton).setVisibility(View.VISIBLE);
                                Toast.makeText(CreateEventActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(CreateEventActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        //get email to access organizer
        session = new Session(this);
        String username = session.getUserEmail();

        ImageButton backButton = findViewById(R.id.createBackButton);

        backButton.setOnClickListener(view ->{
            finish();
        });

        //edit texts and switches
        EditText name = findViewById(R.id.nameEditText);
        EditText date = findViewById(R.id.dateEditText);
        EditText time = findViewById(R.id.timeEditText);
        EditText regStartDate = findViewById(R.id.startDateEditText);
        EditText regEndDate = findViewById(R.id.endDateEditText);
        EditText description = findViewById(R.id.descrEditText);
        EditText location = findViewById(R.id.locEditText);

        SwitchCompat limitEntrants = findViewById(R.id.limitEntrantsSwitch);
        SwitchCompat requireGeoLocation = findViewById(R.id.requireGeoLocationSwitch);
        EditText entrantLimit = findViewById(R.id.maxEntrantsEditText);
        EditText slots = findViewById(R.id.slotsEditText);

        Button imageUploadButton = findViewById(R.id.uploadPosterButton);
        imageUploadButton.setOnClickListener(v -> pickMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        findViewById(R.id.removePosterButton).setOnClickListener(v -> {
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
            }
        });

        entrantLimit.setVisibility(View.GONE);
        limitEntrants.setOnClickListener(view -> {
            if (limitEntrants.isChecked()) {
                entrantLimit.setVisibility(View.VISIBLE);
            } else {
                entrantLimit.setVisibility(View.GONE);
            }
        });

        Button publishButton = findViewById(R.id.publishEventButton);
        publishButton.setOnClickListener(view ->{
            userRepo.getOrganizer(username,
                    user -> {
                        if (user != null) {
                            if(inputsValid(name.getText().toString(),slots.getText().toString())) { //validate inputs before making event
                                Event event = new Event(user);
                                event.setEventName(name.getText().toString()); //get text from edit texts
                                event.setDate(date.getText().toString());
                                event.setTime(time.getText().toString());
                                event.setRegStartDate(regStartDate.getText().toString());
                                event.setRegEndDate(regEndDate.getText().toString());
                                event.setAddress(location.getText().toString());
                                event.setDescription(description.getText().toString());
                                event.setRequireGeolocation(requireGeoLocation.isChecked());
                                if (limitEntrants.isChecked()) {
                                    String limit = entrantLimit.getText().toString();
                                    event.setEntrantLimit(Integer.parseInt(limit)); //make int
                                }
                                if (!imageURL.isEmpty()){
                                    event.setImageUrl(imageURL);
                                }
                                event.setSlots(Integer.parseInt(slots.getText().toString()));


                                eventRepo.addEvent(event);
                                eventSaved = true;
                                finish();
                            }
                        }
                    });



        });

    }

    public void loadImage(){
        ImageView poster = findViewById(R.id.posterImageView);
        Glide.with(this).load(imageURL).into(poster);
    }
    /**
     *  Checks inputs and makes toasts to tell user what is missing or invalid
     * @param name name of event
     * @param slots number of slots event has open
     * todo : check rest of mandatory inputs
       */
    public boolean inputsValid(String name, String slots) {
        boolean valid = true;
        if (name.isEmpty()) { //check if name is empty
            Toast.makeText(this, "Event must have name", Toast.LENGTH_SHORT).show();
            valid = false;
        }else if (slots.isEmpty()) { //check that slots is non negative (else if so it doesn't try to display all toasts at once)
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
                    CreateEventActivity.super.finish();
                }
                @Override
                public void onFailure(String error) {
                    Log.e("CreateEventActivity", "Failed to delete image: " + error);
                    CreateEventActivity.super.finish();
                }
            });
        } else {
            super.finish();
        }
    }



}




