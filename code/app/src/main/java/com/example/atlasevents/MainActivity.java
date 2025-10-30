package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userRepo = new UserRepository();

        Entrant newUser = new Entrant(
                "Alice",
                "alice@example1.com",
                "mypassword",
                "1234567890"
        );

        userRepo.addUser(newUser)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "User added successfully"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to add user", e));
    }

}