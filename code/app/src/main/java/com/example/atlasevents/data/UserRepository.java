package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Entrant;
import com.example.atlasevents.Organizer;
import com.example.atlasevents.PasswordHasher;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;
    private final PasswordHasher passwordHasher;
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        passwordHasher = new PasswordHasher();
    }
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
    }
    public interface OnOrganizerFetchedListener {
        void onOrganizerFetched(Organizer user);
    }
    public interface OnEntrantFetchedListener {
        void onEntrantFetched(Entrant user);
    }
    public interface OnUserUpdatedListener {
        enum UpdateStatus {
            SUCCESS,
            EMAIL_ALREADY_USED,
            FAILURE
        }
        void onUserUpdated(UpdateStatus status);
    }
    public void addUser(@NonNull User user, @NonNull OnUserUpdatedListener listener) {
        String email = user.getEmail();
        user.setPassword(passwordHasher.passHash(user.getPassword()));
        
        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Email already exists
                        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
                    } else {
                        db.collection("users").document(email)
                                .set(user)
                                .addOnSuccessListener(aVoid -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                                .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
                    }
                })
                .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
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

    public void setUser(@NonNull String email, @NonNull User newUser, @NonNull OnUserUpdatedListener listener) {
        String newEmail = newUser.getEmail();
        newUser.setPassword(passwordHasher.passHash(newUser.getPassword()));

        // If the email hasn't changed, just update the existing document
        if (email.equals(newEmail)) {
            db.collection("users").document(email)
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                    .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
        } else {
            db.collection("users")
                    .document(newEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // New email is already used
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
                        } else {
                            db.collection("users").document(newEmail)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid ->
                                            db.collection("users").document(email)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid2 -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                                                    .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE))
                                    )
                                    .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
                        }
                    })
                    .addOnFailureListener(e -> listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
        }
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

//    public void isNotificationsEnabled(String email, NotificationsPrefCallback callback) {
//        db.collection("users").document(email)
//                .get()
//                .addOnSuccessListener(doc -> {
//                    if (doc == null || !doc.exists()) {
//                        callback.onResult(null);
//                    } else {
//                        Boolean enabled = doc.getBoolean("notificationsEnabled");
//                        // default true if null
//                        if (enabled == null) enabled = true;
//                        callback.onResult(enabled);
//                    }
//                })
//                .addOnFailureListener(e -> callback.onResult(null));
//    }
//
//    public Task<Void> setNotificationsEnabled(String email, boolean enabled) {
//        Map<String, Object> update = new HashMap<>();
//        update.put("notificationsEnabled", enabled);
//        return db.collection("users").document(email).set(update, SetOptions.merge());
//    }

    /**
     * This method gets an entrant from the database.
     * @param listener: listener to check for successful database query
     * @param name: email address of the entrant
     */
    public void getEntrant(String name, OnEntrantFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("email", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Entrant entrant = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(Entrant.class);
                        listener.onEntrantFetched(entrant);
                    } else {
                        listener.onEntrantFetched(null); // user not found
                    }
                })
                .addOnFailureListener(e -> listener.onEntrantFetched(null));
    }
    
    /**
     * Callback interface for blocked organizers operations
     */
    public interface BlockedOrganizersCallback {
        void onResult(boolean isBlocked);
        void onFailure(Exception e);
    }
    
    /**
     * Callback interface for checking if an organizer is blocked
     */
    public interface IsBlockedCallback {
        void onResult(boolean isBlocked);
    }
    
    /**
     * Checks if a specific organizer is blocked by the user
     * @param userEmail The email of the user
     * @param organizerEmail The email of the organizer to check
     * @param callback Callback with the result
     */
    public void isOrganizerBlocked(String userEmail, String organizerEmail, IsBlockedCallback callback) {
        db.collection("users")
                .document(userEmail)
                .collection("preferences")
                .document("blockedOrganizers")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        java.util.List<String> blockedEmails = 
                            (java.util.List<String>) documentSnapshot.get("blockedEmails");
                        boolean isBlocked = blockedEmails != null && blockedEmails.contains(organizerEmail);
                        callback.onResult(isBlocked);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }
    
    /**
     * Blocks an organizer from sending notifications to the user
     * @param userEmail The email of the user
     * @param organizerEmail The email of the organizer to block
     * @param callback Callback with success/failure result
     */
    public void blockOrganizer(String userEmail, String organizerEmail, BlockedOrganizersCallback callback) {
        db.collection("users")
                .document(userEmail)
                .collection("preferences")
                .document("blockedOrganizers")
                .update("blockedEmails", com.google.firebase.firestore.FieldValue.arrayUnion(organizerEmail))
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> {
                    // Document might not exist, create it
                    java.util.List<String> blockedList = new java.util.ArrayList<>();
                    blockedList.add(organizerEmail);
                    Map<String, Object> data = new HashMap<>();
                    data.put("blockedEmails", blockedList);
                    
                    db.collection("users")
                            .document(userEmail)
                            .collection("preferences")
                            .document("blockedOrganizers")
                            .set(data)
                            .addOnSuccessListener(aVoid -> callback.onResult(true))
                            .addOnFailureListener(e2 -> callback.onFailure(e2));
                });
    }
    
    /**
     * Unblocks an organizer, allowing them to send notifications to the user again
     * @param userEmail The email of the user
     * @param organizerEmail The email of the organizer to unblock
     * @param callback Callback with success/failure result
     */
    public void unblockOrganizer(String userEmail, String organizerEmail, BlockedOrganizersCallback callback) {
        db.collection("users")
                .document(userEmail)
                .collection("preferences")
                .document("blockedOrganizers")
                .update("blockedEmails", com.google.firebase.firestore.FieldValue.arrayRemove(organizerEmail))
                .addOnSuccessListener(aVoid -> callback.onResult(false))
                .addOnFailureListener(e -> callback.onFailure(e));
    }
}


