package com.example.atlasevents.data;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.*;
import com.example.atlasevents.data.model.Notification;
import java.util.HashMap;
import java.util.Map;

/**
 * OrganiserWaitlistListener:
 * - constructed with eventId
 * - when started, listens to events/{eventId}/waitlist
 * - on ADDED => create a notification for the organizer under users/{organiserUid}/notifications
 * - on REMOVED => create a "left" notification
 *
 * This is the "client-side" mechanism to create notifications — organizer app must run this listener.
 *
 * This implementation writes notifications for the organiser themself
 * Givbing the organiser a historical log and multiple organizer devices can read the same notifications.
 * If you also want organizer to notify other users, adapt createNotificationForOrganizer to write under the recipient's UID
 * — but remember rules must allow that
 */

public class OrganiserWaitlistListener {
    private static final String TAG = "OrganiserWLListener";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;
    private final String eventId;
    private final String organiseremail; // current logged in-user

    public OrganiserWaitlistListener(@NonNull String eventId){
        this.eventId = eventId;
        this.organiseremail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

    }
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

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    // Create notification document under users/{organizerUid}/notifications
    private void createNotificationForOrganizer(String title, String message, DocumentSnapshot waitlistDoc) {
        try {
            CollectionReference notifRef = db.collection("users").document(organiseremail).collection("notifications");
            DocumentReference newNotif = notifRef.document();
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", newNotif.getId());
            payload.put("title", title);
            payload.put("message", message);
            payload.put("eventId", eventId);
            payload.put("fromOrganizerUid", organiseremail); // the writer
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
