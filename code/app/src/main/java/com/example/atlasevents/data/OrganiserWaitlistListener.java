package com.example.atlasevents.data;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.*;
import com.example.atlasevents.data.model.Notification;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens for real-time changes to event waitlists and notifies organizers.
 * Monitors additions and removals from event waitlists and creates notifications
 * for the event organizer when changes occur.
 *
 * <p>This class provides organizers with immediate feedback when users join or leave
 * their event waitlists, enabling timely management of event attendance.</p>
 *
 * @see Notification
 * @see FirebaseFirestore
 * @see ListenerRegistration
 */

public class OrganiserWaitlistListener {
    private static final String TAG = "OrganiserWLListener";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;
    private final String eventId;
    private final String organiseremail; // organizer's email

    /**
     * Constructs a new waitlist listener for the specified event and organizer.
     *
     * @param eventId The unique identifier of the event to monitor
     * @param organizerEmail The email address of the event organizer to notify
     * @throws NullPointerException if eventId or organizerEmail is null
     * @see #start()
     * @see #stop()
     */
    public OrganiserWaitlistListener(@NonNull String eventId, @NonNull String organizerEmail){
        this.eventId = eventId;
        this.organiseremail = organizerEmail;
    }
    /**
     * Starts listening for waitlist changes on the specified event.
     * Attaches a Firestore snapshot listener to the event's waitlist collection.
     * Creates notifications for the organizer when entrants are added or removed.
     *
     * @throws IllegalStateException if Firestore operations fail
     * @see #stop()
     * @see #createNotificationForOrganizer(String, String, DocumentSnapshot)
     */
    public void start() {
        DocumentReference eventRef = db.collection("events").document(eventId);
        CollectionReference waitlistRef = eventRef.collection("waitlist");

        registration = waitlistRef.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listener error", e);
                return;
            }
            if (snapshots == null) return;

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                DocumentSnapshot doc = dc.getDocument();
                String entrantUid = doc.getString("entrantUid") != null ? doc.getString("entrantUid") : doc.getId();

                switch (dc.getType()) {
                    case ADDED:
                        createNotificationForOrganizer("New entrant", entrantUid + " joined your event", doc);
                        break;
                    case REMOVED:
                        createNotificationForOrganizer("Entrant left", entrantUid + " left your event", doc);
                        break;
                    default:
                        // optionally handle MODIFIED
                }
            }
        });
    }
    /**
     * Stops listening for waitlist changes and cleans up resources.
     * Removes the Firestore snapshot listener.
     *
     * @see #start()
     */

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    // Create notification document under users/{organizerUid}/notifications
    /**
     * Creates a notification for the organizer when waitlist changes occur.
     * Writes a notification document to the organizer's notifications collection.
     *
     * @param title The title of the notification
     * @param message The detailed message content
     * @param waitlistDoc The Firestore document that triggered the change
     * @return void
     * @throws Exception If Firestore write operations fail
     * @see FirebaseFirestore
     * @see FieldValue#serverTimestamp()
     */
    private void createNotificationForOrganizer(String title, String message, DocumentSnapshot waitlistDoc) {
        try {
            CollectionReference notifRef = db.collection("users").document(organiseremail).collection("notifications");
            DocumentReference newNotif = notifRef.document();
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", newNotif.getId());
            payload.put("title", title);
            payload.put("message", message);
            payload.put("eventId", eventId);
            payload.put("fromOrganizeremail", organiseremail); // the writer
            payload.put("read", false);
            payload.put("createdAt", FieldValue.serverTimestamp());

            newNotif.set(payload)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification created: " + newNotif.getId()))
                    .addOnFailureListener(err -> Log.w(TAG, "Failed to create notification", err));
        } catch (Exception ex) {
            Log.w(TAG, "createNotificationForOrganizer exception", ex);
        }
    }


}
