package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Organizer;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private FirebaseFirestore db;
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
    }
    public interface OnOrganizerFetchedListener {
        void onOrganizerFetched(Organizer user);
    }
    public Task<Void> addUser(@NonNull User user) {
        return db.collection("users").document(user.getEmail()).set(user);
    }
    /**
     * This method gets a user from the database.
     * GIVES A USER CLASS: SHOULD ONLY BE USED TO CHECK USER INFO LIKE PASS OR USERTYPE ON SIGN-IN
     * to use:
     * userRepo.getUser(username,
     *                     user -> {
     *                         if (user != null) {
     *                         //do stuff with user here
     *                         eg. String name = user.getName()
     *                         }
     * @param listener: listener to check for successful database query
     * @param name: email address of the user
     */
    public void getUser(String name, OnUserFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("email", name)
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
    /**
     * This method gets an organizer from the database.
     * @param listener: listener to check for successful database query
     * @param name: email address of the user
     */
    public void getOrganizer(String name, OnOrganizerFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("email", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Organizer user = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(Organizer.class);
                        listener.onOrganizerFetched(user);
                    } else {
                        listener.onOrganizerFetched(null); // user not found
                    }
                })
                .addOnFailureListener(e -> listener.onOrganizerFetched(null));
    }
    public interface NotificationsPrefCallback {
        void onResult(Boolean enabled);
    }

    public void isNotificationsEnabled(String email, NotificationsPrefCallback callback) {
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        callback.onResult(null);
                    } else {
                        Boolean enabled = doc.getBoolean("notificationsEnabled");
                        // default true if null
                        if (enabled == null) enabled = true;
                        callback.onResult(enabled);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    public Task<Void> setNotificationsEnabled(String email, boolean enabled) {
        Map<String, Object> update = new HashMap<>();
        update.put("notificationsEnabled", enabled);
        return db.collection("users").document(email).set(update, SetOptions.merge());
    }
}


