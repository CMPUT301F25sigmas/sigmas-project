package com.example.atlasevents;

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

public class SignUpActivity extends AppCompatActivity {

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

        UserRepository userRepo = new UserRepository();
        Button createButton = findViewById(R.id.createButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        EditText name = findViewById(R.id.name);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        EditText password = findViewById(R.id.newPassword);
        CheckBox orgCheck = findViewById(R.id.organizerCheckBox);
        CheckBox entrantCheck = findViewById(R.id.entrantCheckBox);

        orgCheck.setOnClickListener(view ->{
            entrantCheck.toggle();
        });
        entrantCheck.setOnClickListener(view ->{
            orgCheck.toggle();
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
                        finish();
                    } else if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED) {
                        Log.e("Firestore", "Email already exists");
                        Toast.makeText(this, "This email is already in use.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firestore", "Failed to add user");
                        Toast.makeText(this, "Failed to add user. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Organizer newUser = new Organizer(userName, userEmail, userPassword, userPhone);
                userRepo.addUser(newUser, status -> {
                    if (status == UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS) {
                        Log.d("Firestore", "User added successfully");
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
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