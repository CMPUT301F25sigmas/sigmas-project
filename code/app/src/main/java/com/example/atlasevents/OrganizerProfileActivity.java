package com.example.atlasevents;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.atlasevents.data.UserRepository;
/**
 * Activity for managing organizer profile information.
 * <p>
 * This activity allows organizers to view and edit their profile details including
 * name, email, phone number, and password. Changes are saved to the repository
 * and the user session is updated accordingly.
 * </p>
 * <p>
 * Extends {@link OrganizerBase} to inherit common organizer functionality and UI elements.
 * </p>
 *
 * @see OrganizerBase
 * @see UserRepository
 */
public class OrganizerProfileActivity extends OrganizerBase {

    private EditText nameEdit, emailEdit, phoneEdit, passwordEdit;
    private ImageView nameEditIcon, emailEditIcon, phoneEditIcon, passwordEditIcon;
    private Button saveButton, cancelButton;

    /**
     * Stores the original email address before any modifications.
     * Used as a key for updating the user in the repository when the email changes.
     */
    private String originalEmail;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components, loads the current user's profile details,
     * and sets up event listeners for user interactions.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.organizer_profile);

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
     * Loads the current user's details from the repository and populates the UI fields.
     * <p>
     * Retrieves user information using the email stored in the session and updates
     * all EditText fields with the corresponding user data. Also stores the original
     * email for later reference when saving changes.
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
     * Sets up click listeners for interactive UI elements.
     * <p>
     * Configures the save button to trigger profile updates and the cancel button
     * to reload original user data, discarding any unsaved changes.
     * </p>
     * <p>
     * Note: Edit icon functionality is currently commented out but would toggle
     * the enabled state of individual fields.
     * </p>
     */
    private void setupListeners() {
//        nameEditIcon.setOnClickListener(v -> toggleEdit(nameEdit, nameEditIcon));
//        emailEditIcon.setOnClickListener(v -> toggleEdit(emailEdit, emailEditIcon));
//        phoneEditIcon.setOnClickListener(v -> toggleEdit(phoneEdit, phoneEditIcon));
//        passwordEditIcon.setOnClickListener(v -> toggleEdit(passwordEdit, passwordEditIcon));
        saveButton.setOnClickListener(v -> saveChanges());
        cancelButton.setOnClickListener(v -> loadUserDetails());
    }

//    private void toggleEdit(EditText editText, ImageView icon) {
//        boolean enabled = !editText.isEnabled();
//        editText.setEnabled(enabled);
//
//        if (enabled) {
//            editText.requestFocus();
//            icon.setImageResource(R.drawable.ic_edit_active);
//        } else {
//            icon.setImageResource(R.drawable.ic_edit);
//        }
//    }

    /**
     * Saves the modified user profile information to the repository.
     * <p>
     * Creates a new {@link Organizer} object with the current field values and
     * attempts to update it in the repository. Handles the following outcomes:
     * </p>
     * <ul>
     *     <li><b>SUCCESS:</b> Displays success message, updates session email if changed,
     *         and updates the original email reference</li>
     *     <li><b>EMAIL_ALREADY_USED:</b> Displays error message indicating the email
     *         is already taken by another user</li>
     *     <li><b>FAILURE:</b> Displays generic error message</li>
     * </ul>
     * <p>
     * The password field is cleared after the save operation regardless of success.
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

                // Update session email if it changed
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
        passwordEdit.setText("");
    }
}
