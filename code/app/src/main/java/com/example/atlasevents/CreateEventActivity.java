package com.example.atlasevents;

import android.os.Bundle;
import android.text.Editable;
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
import com.example.atlasevents.data.UserRepository;

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
    /**
     * Repository for user data operations.
     */
    UserRepository userRepo = new UserRepository();

    /**
     * Repository for event data operations.
     */
    EventRepository eventRepo = new EventRepository();

    /**
     * Current user session containing authentication information.
     */
    Session session;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the event creation form, sets up the UI components including
     * input fields and switches, and configures button listeners for creating
     * and publishing events. Enables edge-to-edge display and applies window insets.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied in onSaveInstanceState.
     *                           Otherwise it is null.
     */
    //    todo: make "limit number of entrants" just a editText instead of a switch and editText
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

        //get email to access organizer
        session = new Session(this);
        String username = session.getUserEmail();

        ImageButton backButton = findViewById(R.id.createBackButton);

        backButton.setOnClickListener(view ->{
            finish();
        });

        //edit texts and switches
        EditText name = findViewById(R.id.nameEditText);
        EditText start = findViewById(R.id.startDateEditText);
        EditText end = findViewById(R.id.endDateEditText);
        EditText description = findViewById(R.id.descrEditText);
        EditText location = findViewById(R.id.locEditText);
        SwitchCompat limitEntrants = findViewById(R.id.limitEntrantsSwitch);
        SwitchCompat requireGeoLocation = findViewById(R.id.requireGeoLocationSwitch);
        EditText entrantLimit = findViewById(R.id.maxEntrantsEditText);
        EditText slots = findViewById(R.id.slotsEditText);

        entrantLimit.setEnabled(false);
        limitEntrants.setOnClickListener(view ->{
            entrantLimit.setEnabled(true);
                });



        Button publishButton = findViewById(R.id.publishEventButton);
        publishButton.setOnClickListener(view ->{
            userRepo.getOrganizer(username,
                    user -> {
                        if (user != null) {
                            if(inputsValid(name.getText().toString(),slots.getText().toString())) { //validate inputs before making event
                                Organizer organizer = user;
                                Event event = new Event(organizer);
                                event.setEventName(name.getText().toString()); //get text from edit texts
                                event.setStart(start.getText().toString());
                                event.setEnd(end.getText().toString());
                                event.setAddress(location.getText().toString());
                                event.setDescription(description.getText().toString());
                                event.setRequireGeolocation(requireGeoLocation.isChecked());
                                if (limitEntrants.isChecked()) {
                                    String limit = entrantLimit.getText().toString();
                                    event.setEntrantLimit(Integer.parseInt(limit)); //make int
                                }
                                event.setSlots(Integer.parseInt(slots.getText().toString()));


                                eventRepo.addEvent(event);
                                finish();
                            }
                        }
                    });



        });

    }
    /**
     * Validates the required inputs for event creation.
     * <p>
     * Checks that the event name is not empty and that the number of slots
     * is provided. Displays appropriate Toast messages to inform the user
     * of any missing or invalid inputs.
     * </p>
     *
     * @param name The name of the event to validate
     * @param slots The number of participant slots as a string
     * @return {@code true} if all inputs are valid, {@code false} otherwise
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




