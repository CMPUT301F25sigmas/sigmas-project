package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

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

        createButton.setOnClickListener(view ->{
            //to do: assert that name,email,and password are not blank
            if(entrantCheck.isChecked()) {
                Entrant newUser = new Entrant(name.getText().toString(), email.getText().toString(), password.getText().toString(), phone.getText().toString());
                userRepo.addUser(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User added successfully");
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Log.e("Firestore", "Failed to add user", e)
                        );
            } else {
                Organizer newUser = new Organizer(name.getText().toString(), email.getText().toString(), password.getText().toString(), phone.getText().toString());
                userRepo.addUser(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "User added successfully");
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Log.e("Firestore", "Failed to add user", e)
                        );
            }
        });

    }
}