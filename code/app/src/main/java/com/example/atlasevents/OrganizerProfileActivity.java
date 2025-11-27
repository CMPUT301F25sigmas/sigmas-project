package com.example.atlasevents;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.atlasevents.data.UserRepository;

/**
 * Activity for managing an organizer's profile information.
 * <p>
 * Allows organizers to view and update their personal details such as
 * name, email, phone number, and password. The updates are reflected in
 * Firebase through {@link UserRepository}.
 * </p>
 */
public class OrganizerProfileActivity extends OrganizerBase {

    private EditText nameEdit, emailEdit, phoneEdit, passwordEdit;
    private ImageView nameEditIcon, emailEditIcon, phoneEditIcon, passwordEditIcon;
    private Button saveButton, cancelButton;

    /** Stores the user's original email to detect changes or updates. */
    private String originalEmail;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the layout, loads user details, and sets up listeners for buttons.
     * </p>
     *
     * @param savedInstanceState The previously saved instance state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_profile);
        setActiveNavItem(R.id.profile_icon);

        nameEdit = findViewById(R.id.name);
        emailEdit = findViewById(R.id.email);
        phoneEdit = findViewById(R.id.phone);
        passwordEdit = findViewById(R.id.password);

        nameEditIcon = findViewById(R.id.nameEditIcon);
        emailEditIcon = findViewById(R.id.emailEditIcon);
        phoneEditIcon = findViewById(R.id.phoneEditIcon);
        passwordEditIcon = findViewById(R.id.passwordEditIcon);

        saveButton = findViewById(R.id.createButton);
        cancelButton = findViewById(R.id.cancelButton);

        loadUserDetails();
        setupListeners();
    }

    /**
     * Loads the current organizer’s details from Firebase via {@link UserRepository}.
     * <p>
     * Populates the editable fields with existing data.
     * </p>
     */
    private void loadUserDetails() {
        userRepository.getUser(session.getUserEmail(), user -> {
            originalEmail = user.getEmail();
            nameEdit.setText(user.getName());
            emailEdit.setText(user.getEmail());
            phoneEdit.setText(user.getPhoneNumber());
        });
    }

    /**
     * Sets up listeners for the save and cancel buttons.
     * <p>
     * Save commits the user changes to Firebase, while cancel reloads the original data.
     * </p>
     */
    private void setupListeners() {
        // Editing icons are currently disabled.
        saveButton.setOnClickListener(v -> saveChanges());
        cancelButton.setOnClickListener(v -> loadUserDetails());
    }

    /**
     * Saves any modifications made by the organizer to Firebase.
     * <p>
     * Updates the user's data through {@link UserRepository#setUser(String, Object, UserRepository.OnUserUpdatedListener)}
     * and provides user feedback via Toast messages.
     * </p>
     * <p>
     * If the email has changed, it also updates the current session.
     * </p>
     */
    private void saveChanges() {
        Organizer newUser = new Organizer(
                nameEdit.getText().toString(),
                emailEdit.getText().toString(),
                passwordEdit.getText().toString(),
                phoneEdit.getText().toString());

        userRepository.setUser(originalEmail, newUser, status -> {
            if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS) {
                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();

                // Update session email if changed
                if (!newUser.getEmail().equals(session.getUserEmail())) {
                    session.setUserEmail(newUser.getEmail());
                }
                originalEmail = newUser.getEmail();
            } else if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED) {
                Toast.makeText(this, "This email is already in use.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save changes. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear password field after saving for security
        passwordEdit.setText("");
    }
}
