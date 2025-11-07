package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Entrant;
import com.example.atlasevents.Organizer;
import com.example.atlasevents.PasswordHasher;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
                        Organizer user = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(Organizer.class);
                        listener.onOrganizerFetched(user);
                    } else {
                        listener.onOrganizerFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onOrganizerFetched(null));
    }

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
                        Entrant entrant = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .toObject(Entrant.class);
                        listener.onEntrantFetched(entrant);
                    } else {
                        listener.onEntrantFetched(null);
                    }
                })
                .addOnFailureListener(e -> listener.onEntrantFetched(null));
    }
}
