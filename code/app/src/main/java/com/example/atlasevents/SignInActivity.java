package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.mindrot.jbcrypt.BCrypt;


import androidx.appcompat.app.AppCompatActivity;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    private UserRepository userRepo;
    private EventRepository eventRepo;
    private Button signInbutton;
    private TextView signUpText;

    private Session session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        userRepo = new UserRepository();
        eventRepo = new EventRepository();
        session = new Session(this);

        if (session.isLoggedIn()) {
            String email = session.getUserEmail();
            userRepo.getUser(email, user -> {
                if (user != null) {
                    Intent intent;
                    if (user.getUserType().equals("Organizer")) {
                        intent = new Intent(SignInActivity.this, OrganizerDashboardActivity.class);
                    } else {
                        intent = new Intent(SignInActivity.this, EntrantDashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    session.logout();
                    setContentView(R.layout.activity_sign_in);
                    signIn();
                }
            });
        } else {
            setContentView(R.layout.activity_sign_in);
            signIn();
        }
    }

    private void signIn() {
        signInbutton = findViewById(R.id.signInButton);
        signUpText = findViewById(R.id.joinNow);
        PasswordHasher passwordHasher = new PasswordHasher();


        EditText usernameField = findViewById(R.id.emailOrPhone);
        EditText passwordField = findViewById(R.id.password);
        signUpText.setOnClickListener(view ->{
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent); //don't close Sign in Activity, will be coming back
        });

        //listener for signInButton
        signInbutton.setOnClickListener(view -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();
            //check email and pass in database
            userRepo.getUser(username,
                    user -> {
                        if (user != null) {
                            if (passwordHasher.checkPass(password,user.getPassword())) { //check pass matches
                                // Save user email to session
                                session.setUserEmail(user.getEmail());
                                
                                if (user.getUserType().equals("Organizer")){//check if user is organizer
                                    Intent intent = new Intent(SignInActivity.this, OrganizerDashboardActivity.class);
                                    session.setUserEmail(user.getEmail());
                                    Bundle bundle = new Bundle();
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                    finish();

                                }
                                //finish();
                                if (user.getUserType().equals("Entrant")){//check if user is entrant
                                    Intent intent = new Intent(SignInActivity.this, EntrantDashboardActivity.class);
                                    session.setUserEmail(user.getEmail());
                                    Bundle bundle = new Bundle();
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                    finish();
                                }


                            }else{
                                Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                            }




                        }else{
                            Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

}