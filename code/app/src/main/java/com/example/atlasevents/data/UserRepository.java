package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class UserRepository {
    private FirebaseFirestore db;
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public Task<Void> addUser(@NonNull User user) {
        return db.collection("users").document(user.getEmail()).set(user);
    }
    public User getUser(String name){
        //to do
        Task<QuerySnapshot> task = db.collection("users").whereEqualTo("name", name).get();
        //i think im supposed to use an onSuccessListener, but idk how to... will fix later
        return task.getResult().getDocuments().get(0).toObject(User.class);

    }

}
