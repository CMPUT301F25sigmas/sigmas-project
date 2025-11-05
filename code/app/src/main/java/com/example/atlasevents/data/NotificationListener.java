package com.example.atlasevents.data;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.*;
import com.example.atlasevents.utils.NotificationHelper;

public class NotificationListener {

    private static final String TAG = "NotificationListener";
    private final FirebaseFirestore db;
    private ListenerRegistration notifsRegistration;
    private ListenerRegistration prefRegistration;
    private final Activity activity;

    private final String email;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public NotificationListener(@NonNull Activity activity, @NonNull String userEmail) {
        this.activity = activity;
        this.db = FirebaseFirestore.getInstance();
        this.email = userEmail;
    }

    /**
     * Start listening for unread notifications for the current user.
     * Displays each new notification via NotificationHelper and marks it read.
     */
    public void start() {
        if (email == null) return;
        // Listen to user doc for notificationsEnabled changes
        DocumentReference userRef = db.collection("users").document(email);
        prefRegistration = userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "pref listener failed", e);
                return;
            }
            if (snapshot == null) return;
            Boolean b = snapshot.getBoolean("notificationsEnabled");
            if (b == null) b = true;
            enabled.set(b);
            if (b) attachNotificationsListener();
            else detachNotificationsListener();
        });
    }

    private void attachNotificationsListener() {
        if (notifsRegistration != null) return;
        CollectionReference notifsRef = db.collection("users").document(email).collection("notifications");
        // Listen to all notifications ordered by createdAt (no index needed)
        notifsRegistration = notifsRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "notification snapshot error", e);
                        return;
                    }
                    if (snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            Notification notif = doc.toObject(Notification.class);
                            if (notif == null) {
                                Log.w(TAG, "Failed to parse notification");
                                continue;
                            }
                            
                            // Only process unread notifications
                            Boolean isRead = doc.getBoolean("read");
                            if (isRead != null && isRead) {
                                Log.d(TAG, "Skipping already read notification: " + doc.getId());
                                continue; // Skip already read notifications
                            }
                            
                            // Double-check enabled
                            if (!enabled.get()) {
                                // skip (but not delete)
                                continue;
                            }
                            String title = notif.getTitle() != null ? notif.getTitle() : "Notification";
                            String message = notif.getMessage() != null ? notif.getMessage() : "";
                            NotificationHelper.showInAppDialog(activity, title, message);
                            doc.getReference().update("read", true)
                                    .addOnFailureListener(err -> Log.w(TAG, "Unable to mark notif read", err));
                        }
                    }
                });
    }

    private void detachNotificationsListener() {
        if (notifsRegistration != null) {
            notifsRegistration.remove();
            notifsRegistration = null;
        }
    }

    public void stop() {
        if (prefRegistration != null) {
            prefRegistration.remove();
            prefRegistration = null;
        }
        detachNotificationsListener();
    }


}

