package com.example.atlasevents.data;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Map;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.*;
import com.example.atlasevents.utils.NotificationHelper;
public class NotificationListener {

    private static final String TAG = "NotificationListener";
    private final FirebaseFirestore db;
    private ListenerRegistration registration;
    private final Activity activity;

    public NotificationListener(@NonNull Activity activity) {
        this.activity = activity;
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Start listening for unread notifications for the current user.
     * Displays each new notification via NotificationHelper and marks it read.
     */
    public void start() {
        //String email = FirebaseAuth.getInstance().getCurrentUser().getemail();
        if (registration != null) return;
        String email = getCurrentUserEmail();
        if (email == null) return;
        CollectionReference notifsRef = db.collection("users").document(email).collection("notifications");

        registration = notifsRef.whereEqualTo("read", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listener error", e);
                        return;
                    }
                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            Notification notif = doc.toObject(Notification.class);
                            if (notif == null) continue;
                            String title = notif.getTitle() != null ? notif.getTitle() : "Notification";
                            String message = notif.getMessage() != null ? notif.getMessage() : "";
                            // Show UI on main thread
                            NotificationHelper.showInAppDialog(activity, title, message);
                            // Mark as read (recipient can also mark manually in UI)
                            doc.getReference().update("read", true)
                                    .addOnFailureListener(err -> Log.w(TAG, "Unable to mark notif read", err));
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

    private String getCurrentUserEmail() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return null;
        // Your auth may use email or uid. Your existing UserRepository uses email as doc id
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }

}

