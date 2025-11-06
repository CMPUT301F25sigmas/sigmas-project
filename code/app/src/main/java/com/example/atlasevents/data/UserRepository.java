package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Organizer;
import com.example.atlasevents.PasswordHasher;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class that handles all database operations related to users and organizers.
 * <p>
 * This class provides methods for adding, fetching, and updating user information in Firestore.
 * It uses asynchronous callbacks via listener interfaces to notify calling classes of query results.
 * </p>
 * <p>
 * Passwords are securely hashed before being stored or updated using {@link PasswordHasher}.
 * </p>
 */
public class UserRepository {
    /** Firestore database instance. */
    private final FirebaseFirestore db;

    /** Handles password hashing for secure storage. */
    private final PasswordHasher passwordHasher;

    /**
     * Constructs a new {@code UserRepository} and initializes Firestore and password hasher.
     */
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        passwordHasher = new PasswordHasher();
    }

    /**
     * Listener interface for retrieving user data asynchronously.
     */
    public interface OnUserFetchedListener {
        /**
         * Called when a user is successfully fetched from the database.
         *
         * @param user The {@link User} object retrieved, or {@code null} if not found.
         */
        void onUserFetched(User user);
    }

    /**
     * Listener interface for retrieving organizer data asynchronously.
     */
    public interface OnOrganizerFetchedListener {
        /**
         * Called when an organizer is successfully fetched from the database.
         *
         * @param user The {@link Organizer} object retrieved, or {@code null} if not found.
         */
        void onOrganizerFetched(Organizer user);
    }

    /**
     * Listener interface for handling user update and add operations.
     */
    public interface OnUserUpdatedListener {
        /**
         * Enum representing possible update or insert operation results.
         */
        enum UpdateStatus {
            /** Operation completed successfully. */
            SUCCESS,
            /** The provided email is already in use. */
            EMAIL_ALREADY_USED,
            /** Operation failed due to a database or connection error. */
            FAILURE
        }

        /**
         * Called when a user update or add operation completes.
         *
         * @param status The {@link UpdateStatus} indicating the operation result.
         */
        void onUserUpdated(UpdateStatus status);
    }

    /**
     * Adds a new user to Firestore if the email address is not already in use.
     * <p>
     * The password is hashed before storage for security.
     * </p>
     *
     * @param user     The {@link User} object containing registration details.
     * @param listener The listener to receive update status callbacks.
     */
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
     * Retrieves a user from the Firestore database based on their email.
     * <p>
     * This method should typically be used to verify credentials or fetch
     * general information (e.g., during sign-in).
     * </p>
     * <p><b>Example usage:</b></p>
     * <pre>
     * userRepo.getUser(email, user -> {
     *     if (user != null) {
     *         String name = user.getName();
     *         // Handle retrieved user data
     *     }
     * });
     * </pre>
     *
     * @param name     The email address of the user.
     * @param listener The listener invoked upon completion with the {@link User} result.
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
     * Updates an existing user's information in Firestore.
     * <p>
     * If the user's email changes, this method ensures the old record is deleted
     * and a new document is created under the updated email.
     * Passwords are always hashed before saving.
     * </p>
     *
     * @param email     The current email (document ID) of the user.
     * @param newUser   The updated {@link User} object.
     * @param listener  The listener invoked with the operation result.
     */
    public void setUser(@NonNull String email,
                        @NonNull User newUser,
                        @NonNull OnUserUpdatedListener listener) {
        String newEmail = newUser.getEmail();
        newUser.setPassword(passwordHasher.passHash(newUser.getPassword()));

        // If the email hasn't changed, just update the existing document
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
                            // New email is already used
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
                                                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE))
                                    )
                                    .addOnFailureListener(e ->
                                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
                        }
                    })
                    .addOnFailureListener(e ->
                            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.FAILURE));
        }
    }

    /**
     * Retrieves an organizer object from Firestore based on their email.
     * <p>
     * This behaves similarly to {@link #getUser(String, OnUserFetchedListener)},
     * but deserializes the data into an {@link Organizer} instead of a {@link User}.
     * </p>
     *
     * @param name     The email address of the organizer.
     * @param listener The listener invoked upon completion with the {@link Organizer} result.
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
}
