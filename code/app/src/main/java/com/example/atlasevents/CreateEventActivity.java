package com.example.atlasevents;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.text.TextUtils;

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
import com.example.atlasevents.utils.DatePickerHelper;
import com.example.atlasevents.utils.ImageUploader;
import com.example.atlasevents.utils.InputValidator;
import com.example.atlasevents.utils.TimePickerHelper;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
/**
 * Activity for creating new events in the Atlas Events system.
 * <p>
 * This activity provides a form interface for organizers to create events with
 * various details including name, dates, location, description, participant limits,
 * and geolocation requirements. The activity validates inputs before creating the
 * event and saving it to the repository.
 * </p>
 *
 * @see Event
 * @see Organizer
 * @see EventRepository
 */
public class CreateEventActivity extends AppCompatActivity {
    UserRepository userRepo = new UserRepository();
    EventRepository eventRepo = new EventRepository();
    Session session;
    boolean eventSaved = false;

    ImageUploader uploader;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> placesAutocompleteLauncher;
    String imageURL = "";
    private LatLng selectedLatLng;
    private String resolvedAddress;
    private final List<String> tags = new ArrayList<>();
    private LinearLayout tagContainer;

    /**
     * The date picker to pick the start date of the event
     */
    private DatePickerHelper startDatePicker;

    /**
     * The date picker to pick the registration period
     */
    private DatePickerHelper registrationPeriodPicker;

    /**
     * The time picker to pick the start time
     */
    private TimePickerHelper timePicker;

    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
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

        initializePlacesSdk();

        startDatePicker = new DatePickerHelper();
        registrationPeriodPicker = new DatePickerHelper(Boolean.TRUE);
        timePicker = new TimePickerHelper();

        uploader = new ImageUploader();

        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        uploader = new ImageUploader();
                        uploader.uploadImage(getContentResolver(), uri, new ImageUploader.UploadCallback() {
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

        placesAutocompleteLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        selectedLatLng = place.getLatLng();
                        resolvedAddress = resolveAddressFromPlace(place);
                        EditText locationField = findViewById(R.id.locEditText);
                        locationField.setText(resolvedAddress);
                    } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR && result.getData() != null) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Log.e("CreateEventActivity", "Autocomplete error: " + status.getStatusMessage());
                        Toast.makeText(this, "Location selection failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        //get email to access organizer
        session = new Session(this);
        String username = session.getUserEmail();

        ImageButton backButton = findViewById(R.id.notificationCentreBackButton);

        backButton.setOnClickListener(view ->{
            finish();
        });

        //edit texts and switches
        EditText name = findViewById(R.id.nameEditText);
        EditText date = findViewById(R.id.dateEditText);
        date.setOnClickListener(v -> {
            startDatePicker.showPicker(getSupportFragmentManager(), (startDate, endDate) -> {
                date.setText(startDatePicker.getStartDateFormatted());
            });
        });

        EditText time = findViewById(R.id.timeEditText);
        time.setOnClickListener(v -> {
            timePicker.showPicker(this, (h, m) -> {
                time.setText(timePicker.getFormattedTime());
            });
        });

        EditText regDateRange = findViewById(R.id.regDateEditText);
        regDateRange.setOnClickListener(v -> {
            registrationPeriodPicker.showPicker(getSupportFragmentManager(), (startDate, endDate) -> {
                String text = registrationPeriodPicker.getStartDateFormatted() + " - " + registrationPeriodPicker.getEndDateFormatted();
                regDateRange.setText(text);
            });
        });

        EditText description = findViewById(R.id.descrEditText);
        EditText location = findViewById(R.id.locEditText);
        location.setFocusable(false);
        location.setOnClickListener(v -> launchPlaceAutocomplete());

        SwitchCompat limitEntrants = findViewById(R.id.limitEntrantsSwitch);
        SwitchCompat requireGeoLocation = findViewById(R.id.requireGeoLocationSwitch);
        EditText entrantLimit = findViewById(R.id.maxEntrantsEditText);
        EditText slots = findViewById(R.id.slotsEditText);
        tagContainer = findViewById(R.id.tagContainer);
        Button editTagsButton = findViewById(R.id.editTagsButton);
        editTagsButton.setOnClickListener(v -> showTagEditor());
        renderTags();

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
                            if(inputsValid(name,slots,limitEntrants.isChecked(), entrantLimit.getText().toString())) { //validate inputs before making event
                                Event event = new Event(user);
                                event.setEventName(name.getText().toString()); //get text from edit texts
                                event.setDate(startDatePicker.getStartDate());
                                event.setTime(timePicker.getFormattedTime());
                                event.setRegStartDate(registrationPeriodPicker.getStartDate());
                                event.setRegEndDate(registrationPeriodPicker.getEndDate());
                                event.setAddress(resolvedAddress != null ? resolvedAddress : location.getText().toString());
                                if (selectedLatLng != null) {
                                    event.setLocation(new GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude));
                                }
                                event.setDescription(description.getText().toString());
                                event.setRequireGeolocation(requireGeoLocation.isChecked());
                                if (limitEntrants.isChecked()) {
                                    String limit = entrantLimit.getText().toString();
                                    event.setEntrantLimit(Integer.parseInt(limit)); //make int
                                }
                                if (!imageURL.isEmpty()){
                                    event.setImageUrl(imageURL);
                                }
                                event.setTags(tags);
                                event.setSlots(Integer.parseInt(slots.getText().toString()));


                                eventRepo.addEvent(event);
                                eventSaved = true;
                                finish();
                            }
                        }
                    });



        });

    }

    /**
     * Loads the image from the provided URL into the ImageView.
     *
     */
    public void loadImage(){
        ImageView poster = findViewById(R.id.posterImageView);
        Glide.with(this).load(imageURL).into(poster);
    }

    /**
     * Opens a simple dialog that lets organizers enter comma-separated tags.
     * Parsed tags are normalized to lowercase and deduplicated before being shown.
     */
    private void showTagEditor() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter tags separated by commas");
        // Limit dialog input to 75 characters
        input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(75) });
        if (!tags.isEmpty()) {
            input.setText(TextUtils.join(", ", tags));
            input.setSelection(input.getText().length());
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit tags")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    List<String> parsed = parseTags(input.getText().toString());
                    tags.clear();
                    tags.addAll(parsed);
                    renderTags();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates the horizontal tag row with chip-style labels or a placeholder when empty.
     */
    private void renderTags() {
        if (tagContainer == null) {
            return;
        }
        tagContainer.removeAllViews();
        if (tags.isEmpty()) {
            TextView placeholder = new TextView(this);
            placeholder.setText("No tags added");
            placeholder.setTextColor(Color.parseColor("#494949"));
            tagContainer.addView(placeholder);
            return;
        }

        int horizontalPadding = toPx(12);
        int verticalPadding = toPx(6);
        int chipRadius = toPx(18);

        for (String tag : tags) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextColor(Color.parseColor("#494949"));
            chip.setPadding(horizontalPadding * 2, verticalPadding, horizontalPadding * 2, verticalPadding);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.parseColor("#E8DEF8"));
            background.setCornerRadius(chipRadius);
            chip.setBackground(background);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, toPx(8), 0);
            chip.setLayoutParams(params);

            tagContainer.addView(chip);
        }
    }

    /**
     * Normalizes comma-separated tags into a unique, lowercase list for storage/search.
     */
    List<String> parseTags(String raw) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (!TextUtils.isEmpty(raw)) {
            String[] pieces = raw.split(",");
            for (String piece : pieces) {
                String cleaned = piece.trim().toLowerCase(Locale.ROOT);
                if (!cleaned.isEmpty()) {
                    parsed.add(cleaned);
                }
            }
        }
        return new ArrayList<>(parsed);
    }

    private int toPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

/**
 * Validates all inputs for event creation including dates and times.
 * <p>
 * Checks that the event name is not empty, number of slots is valid,
 * and all date/time fields are properly selected and in the correct order.
 * Displays appropriate error messages for any validation failures.
 * </p>
 *
 * @param name The EditText containing the event name
 * @param slots The EditText containing the number of slots
 * @param limitEntrants Whether the entrant limit is enabled
 * @param limit The entrant limit value
 * @return {@code true} if all inputs are valid, {@code false} otherwise
 */
public boolean inputsValid(EditText name, EditText slots, boolean limitEntrants, String limit) {
        boolean valid = true;

        // Get references to the date/time input fields
        EditText dateEditText = findViewById(R.id.dateEditText);
        EditText timeEditText = findViewById(R.id.timeEditText);
        EditText regDateRangeEditText = findViewById(R.id.regDateEditText);

        // Clear previous errors
        name.setError(null);
        slots.setError(null);
        dateEditText.setError(null);
        timeEditText.setError(null);
        regDateRangeEditText.setError(null);

        // Validate event name
        InputValidator.ValidationResult nameRes = InputValidator.validateEventName(name.getText().toString());
        if (!nameRes.isValid()) {
            name.setError(nameRes.errorMessage());
            valid = false;
        }

        // Validate slots
        InputValidator.ValidationResult slotRes = InputValidator.validateSlots(slots.getText().toString());
        if (!slotRes.isValid()) {
            slots.setError(slotRes.errorMessage());
            valid = false;
        }
        // Only check entrant limit if slots are valid
        else if (limitEntrants && Integer.parseInt(slots.getText().toString()) > Integer.parseInt(limit)) {
            slots.setError("Waitlist limit cannot be smaller than number of participants");
            valid = false;
        }

        // Validate event date
        InputValidator.ValidationResult dateResult = InputValidator.validateDateSelected(
                startDatePicker.getStartDate(), "Event date");
        if (!dateResult.isValid()) {
            dateEditText.setError(dateResult.errorMessage());
            valid = false;
        }

        // Validate event time
        InputValidator.ValidationResult timeResult = InputValidator.validateTimeSelected(
                timePicker.hour, timePicker.minute, "Event time");
        if (!timeResult.isValid()) {
            timeEditText.setError(timeResult.errorMessage());
            valid = false;
        }

        // Validate registration period
        InputValidator.ValidationResult regDateResult = InputValidator.validateDateSelected(
                registrationPeriodPicker.getStartDate(), "Registration start date");
        if (!regDateResult.isValid()) {
            regDateRangeEditText.setError(regDateResult.errorMessage());
            valid = false;
        }else if (address.isEmpty() || selectedLatLng == null) {
            Toast.makeText(this, "Select a location", Toast.LENGTH_SHORT).show();
            valid = false;
        }else if (limitEntrants && limit.isEmpty()){
            Toast.makeText(this,"Enter a waitlist limit", Toast.LENGTH_LONG).show();
            valid = false;
        }

        regDateResult = InputValidator.validateDateSelected(
                registrationPeriodPicker.getEndDate(), "Registration end date");
        if (!regDateResult.isValid()) {
            regDateRangeEditText.setError(regDateResult.errorMessage());
            valid = false;
        }

        // Only validate date relationships if individual dates are valid
        if (valid) {
            // Validate registration period is valid (end after start)
            InputValidator.ValidationResult regPeriodResult = InputValidator.validateEndDateAfterStartDate(
                    registrationPeriodPicker.getStartDate(),
                    registrationPeriodPicker.getEndDate(),
                    "Registration start date",
                    "Registration end date"
            );
            if (!regPeriodResult.isValid()) {
                regDateRangeEditText.setError(regPeriodResult.errorMessage());
                valid = false;
            }

            // Validate event date is after registration period
            InputValidator.ValidationResult eventDateResult = InputValidator.validateEndDateAfterStartDate(
                    registrationPeriodPicker.getEndDate(),
                    startDatePicker.getStartDate(),
                    "Registration end date",
                    "Event date"
            );
            if (!eventDateResult.isValid()) {
                dateEditText.setError(eventDateResult.errorMessage());
                valid = false;
            }

            // Validate event time is in the future
            InputValidator.ValidationResult futureTimeResult = InputValidator.validateFutureTime(
                    startDatePicker.getStartDate(),
                    timePicker.hour,
                    timePicker.minute,
                    "Event time"
            );
            if (!futureTimeResult.isValid()) {
                timeEditText.setError(futureTimeResult.errorMessage());
                valid = false;
            }
        }

        return valid;
    }

    private void launchPlaceAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this);
        placesAutocompleteLauncher.launch(intent);
    }

    private void initializePlacesSdk() {
        String apiKey = getPlacesApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("CreateEventActivity", "Places API key missing");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
    }

    private String getPlacesApiKey() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("CreateEventActivity", "Failed to read API key", e);
        }
        return "";
    }

    private String resolveAddressFromPlace(Place place) {
        if (place == null) {
            return "";
        }
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            return place.getAddress();
        }
        if (place.getLatLng() != null) {
            return reverseGeocode(place.getLatLng());
        }
        return place.getName() != null ? place.getName() : "";
    }

    private String reverseGeocode(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder builder = new StringBuilder();
                if (address.getFeatureName() != null) builder.append(address.getFeatureName());
                if (address.getThoroughfare() != null) builder.append(", ").append(address.getThoroughfare());
                if (address.getLocality() != null) builder.append(", ").append(address.getLocality());
                if (address.getAdminArea() != null) builder.append(", ").append(address.getAdminArea());
                if (address.getCountryName() != null) builder.append(", ").append(address.getCountryName());
                return builder.toString();
            }
        } catch (IOException e) {
            Log.e("CreateEventActivity", "Reverse geocoding failed", e);
        }
        return "";
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
