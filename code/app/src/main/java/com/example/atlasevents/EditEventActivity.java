package com.example.atlasevents;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.atlasevents.utils.DatePickerHelper;
import com.example.atlasevents.utils.ImageUploader;
import com.example.atlasevents.utils.InputValidator;
import com.example.atlasevents.utils.TimePickerHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

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
    private EditText regDateRange;

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
     * Container for displaying tag chips.
     */
    private LinearLayout tagContainer;

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

    private final List<String> tags = new ArrayList<>();

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

        startDatePicker = new DatePickerHelper();
        registrationPeriodPicker = new DatePickerHelper(Boolean.TRUE);
        timePicker = new TimePickerHelper();

        ImageButton backButton = findViewById(R.id.notificationCentreBackButton);
        backButton.setOnClickListener(view -> finish());

        session = new Session(this);

        nameEditText = findViewById(R.id.nameEditText);
        dateEditText = findViewById(R.id.dateEditText);
        dateEditText.setOnClickListener(v -> {
            startDatePicker.showPicker(getSupportFragmentManager(), (startDate, endDate) -> {
                dateEditText.setText(startDatePicker.getStartDateFormatted());
            });
        });

        timeEditText = findViewById(R.id.timeEditText);
        timeEditText.setOnClickListener(v -> {
            timePicker.showPicker(this, (h, m) -> {
                timeEditText.setText(timePicker.getFormattedTime());
            });
        });

        regDateRange = findViewById(R.id.regDateEditText);
        regDateRange.setOnClickListener(v -> {
            registrationPeriodPicker.showPicker(getSupportFragmentManager(), (startDate, endDate) -> {
                String text = registrationPeriodPicker.getStartDateFormatted() + " - " + registrationPeriodPicker.getEndDateFormatted();
                regDateRange.setText(text);
            });
        });

        descriptionEditText = findViewById(R.id.descrEditText);
        locationEditText = findViewById(R.id.locEditText);
        limitEntrantsSwitch = findViewById(R.id.limitEntrantsSwitch);
        requireGeoLocationSwitch = findViewById(R.id.requireGeoLocationSwitch);
        entrantLimitEditText = findViewById(R.id.maxEntrantsEditText);
        slotsEditText = findViewById(R.id.slotsEditText);
        updateButton = findViewById(R.id.publishEventButton);
        tagContainer = findViewById(R.id.tagContainer);
        Button editTagsButton = findViewById(R.id.editTagsButton);
        editTagsButton.setOnClickListener(v -> showTagEditor());
        renderTags();
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
                startDatePicker.setStartDate(event.getDate());
                dateEditText.setText(event.getDateFormatted());
                timePicker.setTimeFromString(event.getTime());
                timeEditText.setText(event.getTime());
                registrationPeriodPicker.setStartDate(event.getRegStartDate());
                registrationPeriodPicker.setEndDate(event.getRegEndDate());
                String text = event.getRegStartDateFormatted() + " - " + event.getRegEndDateFormatted();
                regDateRange.setText(text);
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

                tags.clear();
                tags.addAll(parseTags(TextUtils.join(", ", event.getTags())));
                renderTags();
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
        if(inputsValid(nameEditText,slotsEditText,limitEntrantsSwitch.isChecked(), entrantLimitEditText.getText().toString())) {
            return;
        }

        currentEvent.setEventName(nameEditText.getText().toString());
        currentEvent.setDate(startDatePicker.getStartDate());
        currentEvent.setTime(timePicker.getFormattedTime());
        currentEvent.setRegStartDate(registrationPeriodPicker.getStartDate());
        currentEvent.setRegEndDate(registrationPeriodPicker.getEndDate());
        currentEvent.setAddress(locationEditText.getText().toString());
        currentEvent.setDescription(descriptionEditText.getText().toString());
        currentEvent.setRequireGeolocation(requireGeoLocationSwitch.isChecked());
        currentEvent.setSlots(Integer.parseInt(slotsEditText.getText().toString()));
        currentEvent.setTags(tags);

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

    /**
     * Opens a dialog that lets organizers enter comma-separated tags for the event.
     */
    private void showTagEditor() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter tags separated by commas");
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
     * Renders chip-style labels for each tag or shows a placeholder if none exist.
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
    private List<String> parseTags(String raw) {
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
