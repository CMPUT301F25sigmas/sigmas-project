package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;

/**
 * Activity for registering new users within the Atlas Events application.
 * <p>
 * This activity allows users to create either an {@link Entrant} or {@link Organizer}
 * account by entering their personal details including name, email, password, and phone number.
 * Once registration succeeds, a new session is created and the user is redirected
 * to their respective dashboard.
 * </p>
 * <p>
 * The activity also ensures that only one user type checkbox can be active at a time
 * and provides input validation feedback through toast messages.
 * </p>
 *
 * @see UserRepository
 * @see Session
 * @see EntrantDashboardActivity
 * @see OrganizerDashboardActivity
 */
public class SignUpActivity extends AppCompatActivity {

    private Session session;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI layout, configures edge-to-edge display, binds all input fields,
     * and sets up event listeners for the create and cancel buttons.
     * </p>
     *
     * @param savedInstanceState the saved state of the activity if it was previously created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signUp), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        session = new Session(this);
        UserRepository userRepo = new UserRepository();
        Button createButton = findViewById(R.id.createButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        EditText name = findViewById(R.id.name);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        EditText password = findViewById(R.id.newPassword);

        CheckBox adminCheck = findViewById(R.id.adminCheckBox);
        CheckBox orgCheck = findViewById(R.id.organizerCheckBox);
        CheckBox entrantCheck = findViewById(R.id.entrantCheckBox);

        adminCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                orgCheck.setChecked(false);
                entrantCheck.setChecked(false);
            }
        });

        orgCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adminCheck.setChecked(false);
                entrantCheck.setChecked(false);
            }
        });

        entrantCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adminCheck.setChecked(false);
                orgCheck.setChecked(false);
            }
        });


        cancelButton.setOnClickListener(view ->{
            finish();
        });

        createButton.setOnClickListener(view -> {
            String userName = name.getText().toString();
            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();
            String userPhone = phone.getText().toString();

            if (entrantCheck.isChecked()) {
                Entrant newUser = new Entrant(userName, userEmail, userPassword, userPhone);
                userRepo.addUser(newUser, status -> {
                    if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS) {
                        Log.d("Firestore", "User added successfully");
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUpActivity.this, EntrantDashboardActivity.class);
                        session.setUserEmail(newUser.getEmail());
                        Bundle bundle = new Bundle();
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED) {
                        Log.e("Firestore", "Email already exists");
                        Toast.makeText(this, "This email is already in use.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firestore", "Failed to add user");
                        Toast.makeText(this, "Failed to add user. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if(orgCheck.isChecked()) {
                Organizer newUser = new Organizer(userName, userEmail, userPassword, userPhone);
                userRepo.addUser(newUser, status -> {
                    if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS) {
                        Log.d("Firestore", "User added successfully");
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUpActivity.this, OrganizerDashboardActivity.class);
                        session.setUserEmail(newUser.getEmail());
                        Bundle bundle = new Bundle();
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED) {
                        Log.e("Firestore", "Email already exists");
                        Toast.makeText(this, "This email is already in use.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firestore", "Failed to add user");
                        Toast.makeText(this, "Failed to add user. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if(adminCheck.isChecked()) {
                Admin newUser = new Admin(userName, userEmail, userPassword, userPhone);
                userRepo.addUser(newUser, status -> {
                    if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS) {
                        Log.d("Firestore", "User added successfully");
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUpActivity.this, AdminDashboardActivity.class);
                        session.setUserEmail(newUser.getEmail());
                        Bundle bundle = new Bundle();
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    } else if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED) {
                        Log.e("Firestore", "Email already exists");
                        Toast.makeText(this, "This email is already in use.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firestore", "Failed to add user");
                        Toast.makeText(this, "Failed to add user. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


    }
}