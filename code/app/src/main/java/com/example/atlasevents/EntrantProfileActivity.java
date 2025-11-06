package com.example.atlasevents;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.atlasevents.EntrantBase;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

public class EntrantProfileActivity extends EntrantBase {

    private EditText nameEdit, emailEdit, phoneEdit, passwordEdit;
    private ImageView nameEditIcon, emailEditIcon, phoneEditIcon, passwordEditIcon;
    private Button saveButton, cancelButton;

    private String originalEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.entrant_profile);

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

    private void loadUserDetails() {
        userRepository.getUser(session.getUserEmail(), user -> {
            originalEmail = user.getEmail();
            nameEdit.setText(user.getName());
            emailEdit.setText(user.getEmail());
            phoneEdit.setText(user.getPhoneNumber());
        });
    }

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

    private void saveChanges() {
        Entrant newUser = new Entrant(
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
