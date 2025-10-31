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
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
    }
    public Task<Void> addUser(@NonNull User user) {
        return db.collection("users").document(user.getEmail()).set(user);
    }
    public void getUser(String name, OnUserFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(User.class);
                        listener.onUserFetched(user);
                    } else {
                        listener.onUserFetched(null); // user not found
                    }
                })
                .addOnFailureListener(e -> listener.onUserFetched(null));
    }
}


