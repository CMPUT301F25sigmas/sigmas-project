package com.example.atlasevents.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.atlasevents.data.model.Invite;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing event invitation operations with Firebase Firestore.
 * <p>
 * This class provides methods for creating, retrieving, updating, and deleting
 * event invitations. Invitations are stored in a separate "invites" collection
 * and are distinct from regular notifications.
 * </p>
 *
 * @see Invite
 */
public class InviteRepository {
    private static final String TAG = "InviteRepository";
    private static final String COLLECTION_NAME = "invites";
    private final FirebaseFirestore db;

    /**
     * Constructs a new InviteRepository with default Firebase Firestore instance.
     */
    public InviteRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Constructs a new InviteRepository with a custom Firebase Firestore instance.
     * For testing.
     *
     * @param db Firebase Firestore instance
     */
    public InviteRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new invitation in Firestore.
     *
     * @param invite The invite object to create
     * @return Task that completes when the invite is created
     */
    public Task<Void> createInvite(@NonNull Invite invite) {
        CollectionReference invitesRef = db.collection(COLLECTION_NAME);
        DocumentReference inviteDoc = invitesRef.document();
        invite.setInviteId(inviteDoc.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("inviteId", invite.getInviteId());
        data.put("eventId", invite.getEventId());
        data.put("recipientEmail", invite.getRecipientEmail());
        data.put("eventName", invite.getEventName());
        data.put("organizerEmail", invite.getOrganizerEmail());
        data.put("status", invite.getStatus());
        data.put("expirationTime", invite.getExpirationTime());
        data.put("message", invite.getMessage());
        data.put("createdAt", FieldValue.serverTimestamp());

        return inviteDoc.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invite created successfully: " + invite.getInviteId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create invite", e);
                });
    }

    /**
     * Creates multiple invitations in parallel.
     *
     * @param invites List of invites to create
     * @return Task containing a list of individual create tasks
     */
    public Task<List<Task<Void>>> createInvites(@NonNull List<Invite> invites) {
        List<Task<Void>> tasks = new ArrayList<>();
        for (Invite invite : invites) {
            tasks.add(createInvite(invite));
        }
        return Tasks.whenAll(tasks).continueWith(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully created " + invites.size() + " invites");
            } else {
                Log.e(TAG, "Failed to create some invites", task.getException());
            }
            return tasks;
        });
    }

    /**
     * Retrieves all pending invitations for a specific user.
     *
     * @param userEmail The email of the user
     * @return Task containing a list of pending invites
     */
    public Task<List<Invite>> getPendingInvitesForUser(@NonNull String userEmail) {
        Log.d(TAG, "Getting pending invites for user: " + userEmail);
        
        // Try with orderBy first, fallback to without orderBy if index is missing
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("recipientEmail", userEmail)
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        Log.w(TAG, "Query with orderBy failed, trying without orderBy", exception);
                        
                        // If it's an index error, try without orderBy
                        if (exception != null && exception.getMessage() != null && 
                            exception.getMessage().contains("index")) {
                            Log.d(TAG, "Retrying query without orderBy due to missing index");
                            return getPendingInvitesForUserWithoutOrderBy(userEmail);
                        }
                        return Tasks.forException(exception);
                    }

                    QuerySnapshot snapshot = task.getResult();
                    List<Invite> invites = processInviteDocuments(snapshot);
                    Log.d(TAG, "Returning " + invites.size() + " valid invites");
                    return Tasks.forResult(invites);
                });
    }
    
    /**
     * Retrieves pending invites without ordering (fallback when index is missing).
     * Results will be sorted in memory.
     */
    private Task<List<Invite>> getPendingInvitesForUserWithoutOrderBy(@NonNull String userEmail) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("recipientEmail", userEmail)
                .whereEqualTo("status", "pending")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Query without orderBy also failed", task.getException());
                        throw task.getException();
                    }

                    QuerySnapshot snapshot = task.getResult();
                    List<Invite> invites = processInviteDocuments(snapshot);
                    
                    // Sort by createdAt in memory (descending)
                    invites.sort((a, b) -> {
                        java.util.Date dateA = a.getCreatedAt();
                        java.util.Date dateB = b.getCreatedAt();
                        if (dateA == null && dateB == null) return 0;
                        if (dateA == null) return 1;
                        if (dateB == null) return -1;
                        return dateB.compareTo(dateA); // Descending order
                    });
                    
                    Log.d(TAG, "Returning " + invites.size() + " valid invites (sorted in memory)");
                    return invites;
                });
    }
    
    /**
     * Helper method to process invite documents from a query snapshot
     */
    private List<Invite> processInviteDocuments(QuerySnapshot snapshot) {
        List<Invite> invites = new ArrayList<>();
        Log.d(TAG, "Processing " + snapshot.size() + " documents");
        
        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
            Invite invite = doc.toObject(Invite.class);
            if (invite != null) {
                invite.setInviteId(doc.getId());
                // Filter out expired invites
                if (!invite.isExpired()) {
                    invites.add(invite);
                } else {
                    // Auto-expire expired invites
                    updateInviteStatus(doc.getId(), "expired");
                }
            }
        }
        return invites;
    }

    /**
     * Retrieves a specific invite by ID.
     *
     * @param inviteId The ID of the invite
     * @return Task containing the invite, or null if not found
     */
    public Task<Invite> getInviteById(@NonNull String inviteId) {
        return db.collection(COLLECTION_NAME)
                .document(inviteId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        Invite invite = doc.toObject(Invite.class);
                        if (invite != null) {
                            invite.setInviteId(doc.getId());
                        }
                        return invite;
                    }
                    return null;
                });
    }

    /**
     * Retrieves an invite by event ID and recipient email.
     *
     * @param eventId The event ID
     * @param recipientEmail The recipient email
     * @return Task containing the invite, or null if not found
     */
    public Task<Invite> getInviteByEventAndRecipient(@NonNull String eventId, @NonNull String recipientEmail) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("recipientEmail", recipientEmail)
                .whereEqualTo("status", "pending")
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    QuerySnapshot snapshot = task.getResult();
                    if (!snapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        Invite invite = doc.toObject(Invite.class);
                        if (invite != null) {
                            invite.setInviteId(doc.getId());
                        }
                        return invite;
                    }
                    return null;
                });
    }

    /**
     * Updates the status of an invite.
     *
     * @param inviteId The ID of the invite
     * @param status The new status ("pending", "accepted", "declined", "expired")
     * @return Task that completes when the status is updated
     */
    public Task<Void> updateInviteStatus(@NonNull String inviteId, @NonNull String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        return db.collection(COLLECTION_NAME)
                .document(inviteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invite status updated: " + inviteId + " -> " + status);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update invite status", e);
                });
    }

    /**
     * Deletes an invite.
     *
     * @param inviteId The ID of the invite to delete
     * @return Task that completes when the invite is deleted
     */
    public Task<Void> deleteInvite(@NonNull String inviteId) {
        return db.collection(COLLECTION_NAME)
                .document(inviteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invite deleted: " + inviteId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete invite", e);
                });
    }

    /**
     * Deletes an invite by event ID and recipient email.
     *
     * @param eventId The event ID
     * @param recipientEmail The recipient email
     * @return Task that completes when the invite is deleted
     */
    public Task<Void> deleteInviteByEventAndRecipient(@NonNull String eventId, @NonNull String recipientEmail) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("recipientEmail", recipientEmail)
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    QuerySnapshot snapshot = task.getResult();
                    if (!snapshot.isEmpty()) {
                        String inviteId = snapshot.getDocuments().get(0).getId();
                        return deleteInvite(inviteId);
                    }
                    return Tasks.forResult(null);
                });
    }

    /**
     * Counts the number of pending invites for a user.
     *
     * @param userEmail The email of the user
     * @return Task containing the count of pending invites
     */
    public Task<Integer> countPendingInvitesForUser(@NonNull String userEmail) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("recipientEmail", userEmail)
                .whereEqualTo("status", "pending")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    QuerySnapshot snapshot = task.getResult();
                    int count = 0;
                    long currentTime = System.currentTimeMillis();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long expirationTime = doc.getLong("expirationTime");
                        if (expirationTime != null && expirationTime > currentTime) {
                            count++;
                        }
                    }
                    return count;
                });
    }
}


