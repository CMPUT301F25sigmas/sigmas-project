package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

public class ActivitySignIn extends AppCompatActivity {
    private UserRepository userRepo;
    private EventRepository eventRepo;
    private Button signInbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        userRepo = new UserRepository();
        eventRepo = new EventRepository();
        signInbutton = findViewById(R.id.signInButton);
        EditText usernameField = findViewById(R.id.emailOrPhone);
        EditText passwordField = findViewById(R.id.password);

        Entrant newUser = new Entrant(
                "Alice",
                "alice@example1.com",
                "mypassword",
                "1234567890"
        );

        Organizer newOrg = new Organizer(
                "Alice",
                "alice@example1.com",
                "mypassword",
                "1234567890"
        );

        Event newEvent = new Event(newOrg);

        userRepo.addUser(newUser)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "User added successfully"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to add user", e));

        eventRepo.addEvent(newEvent)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Event added successfully"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to add event", e));

        //listener for signInButton
        signInbutton.setOnClickListener(view -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();
            //check email and pass in database
            userRepo.getUser(username,
                    user -> {
                        if (user != null) {
                            if (password.equals(user.getPassword())) { //check pass matches
                                //check if user is organizer
                                //Intent intent = new Intent(ActivitySignUp.this, OrganizerDashboard.class);
                                //startActivity(intent);
                                //finish();
                                //check if user is entrant
                                //Intent intent = new Intent(ActivitySignUp.this, EntrantDashboard.class);
                                //startActivity(intent);
                                //finish();

                            }




                        }
                    });
        });
    }

}