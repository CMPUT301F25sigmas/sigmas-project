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

/**
 * Activity responsible for handling user authentication within the Atlas Events application.
 * <p>
 * This activity verifies user credentials against stored data using {@link UserRepository}
 * and manages user sessions through {@link Session}. It directs authenticated users to
 * their respective dashboards — either {@link OrganizerDashboardActivity} or
 * {@link EntrantDashboardActivity} — based on their user type.
 * </p>
 * <p>
 * If a valid session already exists, the user is automatically redirected to the
 * appropriate dashboard. Otherwise, the activity displays the sign-in screen.
 * </p>
 *
 * @see UserRepository
 * @see EventRepository
 * @see Session
 * @see OrganizerDashboardActivity
 * @see EntrantDashboardActivity
 */
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
                    } else if(user.getUserType().equals("Entrant")) {
                        intent = new Intent(SignInActivity.this, EntrantDashboardActivity.class);
                    } else {
                        intent = new Intent(SignInActivity.this, AdminDashboardActivity.class);
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

    /**
     * Initializes the sign-in form and event listeners for user actions.
     * <p>
     * This method sets up the login form UI, validates user input, and performs
     * authentication by checking credentials via {@link UserRepository}.
     * It also provides navigation to the {@link SignUpActivity} for new users.
     * </p>
     * <p>
     * If authentication is successful, the user's email is stored in the {@link Session},
     * and they are redirected to the appropriate dashboard based on their role.
     * </p>
     */
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
                             session.setUserEmail(user.getEmail());
                                switch (user.getUserType()) {
                                    case "Organizer": {//check if user is organizer
                                        Intent intent = new Intent(SignInActivity.this, OrganizerDashboardActivity.class);
                                        session.setUserEmail(user.getEmail());
                                        Bundle bundle = new Bundle();
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        finish();

                                        break;
                                    }
                                    //finish();
                                    case "Entrant": {//check if user is entrant
                                        Intent intent = new Intent(SignInActivity.this, EntrantDashboardActivity.class);
                                        session.setUserEmail(user.getEmail());
                                        Bundle bundle = new Bundle();
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        finish();
                                        break;
                                    }
                                    case "Admin": {//check if user is admin
                                        Intent intent = new Intent(SignInActivity.this, AdminDashboardActivity.class);
                                        session.setUserEmail(user.getEmail());
                                        Bundle bundle = new Bundle();
                                        intent.putExtras(bundle);
                                        startActivity(intent);
                                        finish();
                                        break;
                                    }
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