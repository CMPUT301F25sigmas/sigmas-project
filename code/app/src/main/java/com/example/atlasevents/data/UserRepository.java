package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private FirebaseFirestore db;
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public Task<Void> addUser(@NonNull User user) {
        return db.collection("users").document(user.getEmail()).set(user);
    }

}
