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
import com.example.atlasevents.utils.InputValidator;

/**
 * Activity for registering new users within the Atlas Events application.
 * <p>
 * This activity allows users to create a regular account (able to join or organize events)
 * or an {@link Admin} account by entering their personal details including name, email,
 * password, and phone number. Once registration succeeds, a new session is created and the
 * user is redirected to their dashboard.
 * </p>
 * <p>
 * The flow defaults to a regular user; checking the admin box provisions an admin account.
 * Input validation feedback is provided through toast messages.
 * </p>
 *
 * @see UserRepository
 * @see Session
 * @see EntrantDashboardActivity
 */
public class SignUpActivity extends AppCompatActivity {

    private Session session;

    UserRepository userRepo = new UserRepository();

    // Package-private setter for testing
    void setUserRepository(UserRepository repository) {
        this.userRepo = repository;
    }


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
        Button createButton = findViewById(R.id.createButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        EditText name = findViewById(R.id.name);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        EditText password = findViewById(R.id.newPassword);

        CheckBox adminCheck = findViewById(R.id.adminCheckBox);


        cancelButton.setOnClickListener(view ->{finish();});


        createButton.setOnClickListener(view -> {
            String userName = name.getText().toString();
            String userEmail = email.getText().toString();
            String userPassword = password.getText().toString();
            String userPhone = phone.getText().toString();

            InputValidator.ValidationResult nameRes  = InputValidator.validateName(userName);
            InputValidator.ValidationResult emailRes = InputValidator.validateEmail(userEmail);
            InputValidator.ValidationResult passRes  = InputValidator.validatePassword(userPassword);
            InputValidator.ValidationResult phoneRes = InputValidator.validatePhone(userPhone, false);

            if (!nameRes.isValid())  { name.setError(nameRes.errorMessage()); return;}
            if (!emailRes.isValid()) { email.setError(emailRes.errorMessage());return;}
            if (!phoneRes.isValid()) { phone.setError(phoneRes.errorMessage());return;}
            if (!passRes.isValid())  { password.setError(passRes.errorMessage());return;}



            if(adminCheck.isChecked()) {
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
            else {
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
            }
        });

    }

}
