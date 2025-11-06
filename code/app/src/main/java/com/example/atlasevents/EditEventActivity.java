package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.EventRepository;

public class EditEventActivity extends AppCompatActivity {
    public static final String EventKey = "com.example.atlasevents.EVENT";
    EventRepository eventRepo = new EventRepository();
    Session session;
    private Event currentEvent;
    private String eventId;

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

        backButton.setOnClickListener(view ->{
            finish();
        });

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
        limitEntrantsSwitch.setOnClickListener(view -> {
            entrantLimitEditText.setEnabled(limitEntrantsSwitch.isChecked());
            if (!limitEntrantsSwitch.isChecked()) {
                entrantLimitEditText.setText("");
            }
        });
        updateButton.setOnClickListener(view -> updateEvent());

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
                    entrantLimitEditText.setEnabled(true);
                    entrantLimitEditText.setText(String.valueOf(event.getEntrantLimit()));
                } else {
                    limitEntrantsSwitch.setChecked(false);
                    entrantLimitEditText.setEnabled(false);
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
        if (!inputsValid(nameEditText.getText().toString(),
                slotsEditText.getText().toString())) {
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
            currentEvent.setEntrantLimit(0);
        }

        eventRepo.updateEvent(currentEvent, success -> {
            if (success) {
                Toast.makeText(EditEventActivity.this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditEventActivity.this, "Failed to update event", Toast.LENGTH_SHORT).show();
            }
        });
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
}