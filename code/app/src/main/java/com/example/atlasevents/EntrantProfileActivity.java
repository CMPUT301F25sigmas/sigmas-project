package com.example.atlasevents;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.atlasevents.EntrantBase;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.example.atlasevents.utils.InputValidator;

/**
 * Activity for displaying and editing entrant profile information.
 * <p>
 * This activity extends {@link EntrantBase} and provides a user interface for viewing
 * and modifying profile details including name, email, phone number, and password.
 * Changes are persisted through the {@link UserRepository} and the session is updated
 * if the email address is changed.
 * </p>
 *
 * @see EntrantBase
 * @see UserRepository
 */
public class EntrantProfileActivity extends EntrantBase {

    /**
     * EditText fields for editing profile details.
     */
    private EditText nameEdit, emailEdit, phoneEdit, passwordEdit;
    private ImageView nameEditIcon, emailEditIcon, phoneEditIcon, passwordEditIcon;
    /**
     * Buttons for saving and canceling changes.
     */
    private Button saveButton, cancelButton, deleteProfileButton;

    /**
     * string for original email address
     */
    private String originalEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_profile);
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
        deleteProfileButton = findViewById(R.id.deleteProfileButton);

        loadUserDetails();
        setupListeners();
    }

    /**
     * Loads the current user's details from the repository and populates the UI fields.
     * <p>
     * Retrieves the user object using the email stored in the session and updates
     * all EditText fields with the user's information. The original email is stored
     * for later reference when saving changes.
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
     * Sets up click listeners for the save and cancel buttons.
     * <p>
     * The save button triggers {@link #saveChanges()} to persist profile modifications.
     * The cancel button triggers {@link #loadUserDetails()} to reload the original
     * user information and discard any unsaved changes.
     * </p>
     */
    private void setupListeners() {
//        nameEditIcon.setOnClickListener(v -> toggleEdit(nameEdit, nameEditIcon));
//        emailEditIcon.setOnClickListener(v -> toggleEdit(emailEdit, emailEditIcon));
//        phoneEditIcon.setOnClickListener(v -> toggleEdit(phoneEdit, phoneEditIcon));
//        passwordEditIcon.setOnClickListener(v -> toggleEdit(passwordEdit, passwordEditIcon));
        saveButton.setOnClickListener(v -> saveChanges());
        cancelButton.setOnClickListener(v -> loadUserDetails());
        deleteProfileButton.setOnClickListener(v -> {
            userRepository.deleteUser(session.getUserEmail());
            session.logoutAndRedirect(this);
            Toast.makeText(this, "Successfully deleted profile.", Toast.LENGTH_SHORT).show();
        });
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
     * Creates a new {@link Entrant} object with the current field values and attempts
     * to update the user record. Handles the following outcomes:
     * </p>
     * <ul>
     *   <li><b>SUCCESS</b>: Displays success message, updates session email if changed,
     *       and stores the new email as the original email</li>
     *   <li><b>EMAIL_ALREADY_USED</b>: Displays error message indicating the email
     *       is already in use by another account</li>
     *   <li><b>Other failures</b>: Displays generic error message</li>
     * </ul>
     * <p>
     * After the save attempt, the password field is cleared for security purposes.
     * </p>
     */
    private void saveChanges() {
        String userName = nameEdit.getText().toString();
        String userEmail = emailEdit.getText().toString();
        String userPassword = passwordEdit.getText().toString();
        String userPhone = phoneEdit.getText().toString();
        InputValidator.ValidationResult nameRes  = InputValidator.validateName(userName);
        InputValidator.ValidationResult emailRes = InputValidator.validateEmail(userEmail);
        InputValidator.ValidationResult passRes  = InputValidator.validatePasswordOptional(userPassword);
        InputValidator.ValidationResult phoneRes = InputValidator.validatePhone(userPhone, false);

        if (!nameRes.isValid())  { nameEdit.setError(nameRes.errorMessage()); return;}
        if (!emailRes.isValid()) { emailEdit.setError(emailRes.errorMessage());return;}
        if (!phoneRes.isValid()) { phoneEdit.setError(phoneRes.errorMessage());return;}
        if (!passRes.isValid())  { passwordEdit.setError(passRes.errorMessage());return;}
        Entrant newUser = new Entrant(
                userName,
                userEmail,
                userPassword,
                userPhone);


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
