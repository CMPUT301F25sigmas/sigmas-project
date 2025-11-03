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

public class CreateEventActivity extends AppCompatActivity {
    UserRepository userRepo = new UserRepository();
    EventRepository eventRepo = new EventRepository();
    Session session;
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

        session = new Session(this);
        String username = session.getUserEmail();

        ImageButton backButton = findViewById(R.id.createBackButton);

        backButton.setOnClickListener(view ->{
            finish();
        });
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
                            if(inputsValid(name.getText().toString(),slots.getText().toString())) {
                                Organizer organizer = user;
                                Event event = new Event(organizer);
                                event.setEventName(name.getText().toString());
                                event.setStart(start.getText().toString());
                                event.setEnd(end.getText().toString());
                                event.setAddress(location.getText().toString());
                                event.setDescription(description.getText().toString());
                                event.setRequireGeolocation(requireGeoLocation.isChecked());
                                if (limitEntrants.isChecked()) {
                                    String limit = entrantLimit.getText().toString();
                                    event.setEntrantLimit(Integer.parseInt(limit));
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
     *  Checks inputs and makes toasts to tell user what is missing or invalid
     * @param name name of event
     * @param slots number of slots event has open
     * todo : check rest of inputs
       */
    public boolean inputsValid(String name, String slots) {
        boolean valid = true;
        if (name.isEmpty()) { //check if name is empty
            Toast.makeText(this, "Event must have name", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (slots.isEmpty()) { //check that slots is non negative
            Toast.makeText(this, "Number of participants can not be empty", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }


}




