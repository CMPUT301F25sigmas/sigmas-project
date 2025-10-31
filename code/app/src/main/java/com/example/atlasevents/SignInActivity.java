package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

public class SignInActivity extends AppCompatActivity {
    private UserRepository userRepo;
    private EventRepository eventRepo;
    private Button signInbutton;
    private TextView signUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        userRepo = new UserRepository();
        eventRepo = new EventRepository();
        signInbutton = findViewById(R.id.signInButton);
        signUpText = findViewById(R.id.joinNow);

        EditText usernameField = findViewById(R.id.emailOrPhone);
        EditText passwordField = findViewById(R.id.password);

        Entrant newUser = new Entrant(
                "Alice",
                "alice@example1.com",
                "mypassword",
                "1234567890"
        );

        Organizer newOrg = new Organizer(
                "Bob",
                "bob@example1.com",
                "mypassword",
                "1234567890"
        );

        Event newEvent = new Event(newOrg);

        userRepo.addUser(newOrg)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "User added successfully"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to add user", e));

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

        signUpText.setOnClickListener(view ->{
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        //listener for signInButton
        signInbutton.setOnClickListener(view -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();
            //check email and pass in database
            userRepo.getUser(username,
                    user -> {
                        if (user != null) {
                            if (password.equals(user.getPassword())) { //check pass matches
                                if (user.getUserType().equals("Organizer")){//check if user is organizer
                                    Intent intent = new Intent(SignInActivity.this, OrganizerDashboardActivity.class);
                                    startActivity(intent);
                                    finish();

                                }
                                //finish();
                                if (user.getUserType().equals("Entrant")){//check if user is entrant
                                    Intent intent = new Intent(SignInActivity.this, UserDashboardActivity.class);
                                    startActivity(intent);
                                    finish();

                                }


                            }




                        }
                    });
        });
    }

}