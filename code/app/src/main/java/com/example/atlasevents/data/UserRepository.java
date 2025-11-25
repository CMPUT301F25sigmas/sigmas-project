package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Entrant;
import com.example.atlasevents.Event;
import com.example.atlasevents.Organizer;
import com.example.atlasevents.PasswordHasher;
import com.example.atlasevents.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class for managing User-related data operations in Firebase Firestore.
 * <p>
 * This class provides CRUD operations (Create, Read, Update, Delete) for users,
 * including organizers and entrants. It handles data hashing (for passwords)
 * and ensures that email uniqueness is maintained when adding or updating users.
 * </p>
 */
public class UserRepository {

    private final FirebaseFirestore db;
    private final PasswordHasher passwordHasher;

    /**
     * Constructs a new {@code UserRepository} instance with Firestore and PasswordHasher initialized.
     */
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        passwordHasher = new PasswordHasher();
    }

    /**
     * Listener for retrieving a general {@link User} object from Firestore.
     */
    public interface OnUserFetchedListener {
        void onUserFetched(User user);
    }

    /**
     * Callback interface for operations that return a list of users.
     */
    public interface UsersCallback {
        /**
         * Called when the event retrieval operation succeeds.
         *
         * @param users A list of {@link Event} objects.
         */
        void onSuccess(ArrayList<User> users);

        /**
         * Called when the event retrieval operation fails.
         *
         * @param e The exception thrown.
         */
        void onFailure(Exception e);
    }

    /**
     * Listener for retrieving an {@link Organizer} object from Firestore.
     */
    public interface OnOrganizerFetchedListener {
        void onOrganizerFetched(Organizer user);
    }

    /**
     * Listener for retrieving an {@link Entrant} object from Firestore.
     */
    public interface OnEntrantFetchedListener {
        void onEntrantFetched(Entrant user);
    }

    /**
     * Listener for user update operations, providing detailed status codes.
     */
    public interface OnUserUpdatedListener {

        /**
         * Enumeration representing possible outcomes of user update operations.
         */
        enum UpdateStatus {
            SUCCESS,
            EMAIL_ALREADY_USED,
            FAILURE
        }

        /**
         * Called when a user update or creation operation completes.
         *
         * @param status the result status of the operation.
         */
        void onUserUpdated(UpdateStatus status);
    }

    /**
     * Adds a new user to Firestore if the email does not already exist.
     * The user's password is hashed before being stored.
     *
     * @param user      the {@link User} object to add.
     * @param listener  callback invoked with operation status.
     */
    public void addUser(@NonNull User user, @NonNull OnUserUpdatedListener listener) {
        String email = user.getEmail();
        user.setPassword(passwordHasher.passHash(user.getPassword()));

        db.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
                    } else {
                        db.collection("users").document(email)
                                .set(user)
                                .addOnSuccessListener(aVoid ->
                                        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                                .addOnFailureListener(e ->
                                        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
                    }
                })
                .addOnFailureListener(e ->
                        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
    }

    /**
     * Retrieves a general {@link User} object from Firestore by email.
     * <p>
     * Typically used during sign-in to validate user credentials and determine user type.
     * </p>
     *
     * @param name      the email address of the user.
     * @param listener  callback invoked when the user data is fetched.
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
                        listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onUserFetched(null));
    }

    /**
     * Fetches users from Firestore with the matching user type.
     *
     * @param callback The {@link UsersCallback} to handle success or failure.
     */
    public void getUsers(String type, UsersCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user == null) {
                            continue;
                        }
                        if ("Admin".equals(type)) {
                            if ("Admin".equals(user.getUserType())) {
                                users.add(user);
                            }
                        } else {
                            // Organizer/Entrant both map to the shared non-admin user list.
                            if (!"Admin".equals(user.getUserType())) {
                                users.add(user);
                            }
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates user information in Firestore.
     * <p>
     * If the email is changed, this method checks whether the new email already exists.
     * The old document is deleted only if the update is successful.
     * </p>
     *
     * @param email     the original email of the user.
     * @param newUser   a {@link User} object containing updated details.
     * @param listener  callback invoked with update status.
     */
    public void setUser(@NonNull String email, @NonNull User newUser, @NonNull OnUserUpdatedListener listener) {
        String newEmail = newUser.getEmail();
        newUser.setPassword(passwordHasher.passHash(newUser.getPassword()));

        if (email.equals(newEmail)) {
            db.collection("users").document(email)
                    .set(newUser)
                    .addOnSuccessListener(aVoid ->
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                    .addOnFailureListener(e ->
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
        } else {
            db.collection("users")
                    .document(newEmail)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
                        } else {
                            db.collection("users").document(newEmail)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid ->
                                            db.collection("users").document(email)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid2 ->
                                                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS))
                                                    .addOnFailureListener(e ->
                                                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE)))
                                    .addOnFailureListener(e ->
                                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
                        }
                    })
                    .addOnFailureListener(e ->
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
        }
    }

    /**
     * Retrieves an {@link Organizer} object from Firestore by email.
     *
     * @param name      the email address of the organizer.
     * @param listener  callback invoked with the retrieved organizer or {@code null} if not found.
     */
    public void getOrganizer(String name, OnOrganizerFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("email", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(User.class);
                        listener.onOrganizerFetched(asOrganizer(user));
                    } else {
                        listener.onOrganizerFetched(null);
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
     * Retrieves an {@link Entrant} object from Firestore by email.
     *
     * @param name      the email address of the entrant.
     * @param listener  callback invoked with the retrieved entrant or {@code null} if not found.
     */
    public void getEntrant(String name, OnEntrantFetchedListener listener) {
        db.collection("users")
                .whereEqualTo("email", name)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(User.class);
                        listener.onEntrantFetched(asEntrant(user));
                    } else {
                        listener.onEntrantFetched(null);
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

    /**
     * Deletes a user from Firestore by their email.
     *
     * @param userEmail The unique email of the user to delete.
     */
    public void deleteUser(String userEmail) {
        db.collection("users")
                .document(userEmail)
                .delete();
    }

    /**
     * Converts a generic {@link User} into an {@link Organizer} while keeping existing fields.
     */
    private Organizer asOrganizer(User user) {
        if (user == null) {
            return null;
        }
        Organizer organizer = new Organizer();
        organizer.setName(user.getName());
        organizer.setEmail(user.getEmail());
        organizer.setPassword(user.getPassword());
        organizer.setPhoneNumber(user.getPhoneNumber());
        return organizer;
    }

    /**
     * Converts a generic {@link User} into an {@link Entrant} while keeping existing fields.
     */
    private Entrant asEntrant(User user) {
        if (user == null) {
            return null;
        }
        Entrant entrant = new Entrant();
        entrant.setName(user.getName());
        entrant.setEmail(user.getEmail());
        entrant.setPassword(user.getPassword());
        entrant.setPhoneNumber(user.getPhoneNumber());
        return entrant;
    }
}
