/***
 * @Muaaz Saif
 * @version 1
 * Repository class for managing notification operations with Firebase Firestore.
 * Handles sending notifications to users, logging notification activities, and retrieving notification logs.
 *
 * <p>This class provides methods for sending notifications to individual users, multiple users,
 * and specific groups (waitlist, invited, cancelled). It also handles user notification preferences
 * and maintains an audit log of all notification activities.</p>
 *
 * @see Notification
 * @see NotificationListener
 * @see FirebaseFirestore
 */

package com.example.atlasevents.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.atlasevents.data.model.Notification;
import com.example.atlasevents.Event;

import com.example.atlasevents.Entrant;
import com.example.atlasevents.EntrantList;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.example.atlasevents.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {
    private static final String TAG = "NotificationRepo";
    private final FirebaseFirestore db;

    /**
     * Constructs a new NotificationRepository with default Firebase Firestore instance.
     *
     * @see FirebaseFirestore#getInstance()
     */

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a notification to a single user after checking their notification preferences.
     * If the user has opted out of notifications, the notification is logged but not delivered to their collection.
     *
     * @param userEmail The email address of the recipient user
     * @param notification The notification object to send
     * @return A Task that completes when the notification is processed (sent or logged as opted-out)
     * @throws Exception If the user document cannot be retrieved or if Firestore operations fail
     * @see #logNotification(String, Notification, String)
     * @see Notification
     */

    // send to a single user (if they haven't opted out)
    public Task<Void> sendToUser(@NonNull String userEmail, @NonNull Notification notification) {
        DocumentReference userRef = db.collection("users").document(userEmail);

        return userRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            DocumentSnapshot userDoc = task.getResult();
            if (userDoc == null) throw new Exception("User doc missing");
            Boolean enabled = userDoc.getBoolean("notificationsEnabled");
            if (enabled != null && !enabled) {
                // user has opted out; still write log for admin but don't push notification into their subcollection
                return logNotification(userEmail, notification, "OPTED_OUT");
            }
            // create notification doc under users/{email}/notifications
            CollectionReference notifCol = userRef.collection("notifications");
            DocumentReference notifDoc = notifCol.document();
            notification.setNotificationId(notifDoc.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("notificationId", notification.getNotificationId());
            data.put("title", notification.getTitle());
            data.put("message", notification.getMessage());
            data.put("eventId", notification.getEventId());
            data.put("fromOrganizeremail", notification.getFromOrganizeremail());
            data.put("read", false);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("groupType", notification.getGroupType());
            data.put("eventName", notification.getEventName());


            // perform write and then log
            return notifDoc.set(data).continueWithTask(setTask -> {
                if (!setTask.isSuccessful()) throw setTask.getException();
                return logNotification(userEmail, notification, "SENT");
            });
        }).addOnFailureListener(e -> Log.w(TAG, "sendToUser failure", e));
    }

    // send to multiple users (fire off parallel tasks)
    /**
     * Sends a notification to multiple users in parallel.
     * Creates individual send tasks for each user and returns a combined task that completes when all are done.
     *
     * @param userEmails List of email addresses to send the notification to
     * @param notification The notification object to send (will be copied for each user)
     * @return A Task containing a list of individual send tasks that complete when all notifications are processed
     * @see #sendToUser(String, Notification)
     * @see Tasks.whenAll(tasks)
     */
    public Task<List<Task<Void>>> sendToUsers(@NonNull List<String> userEmails, @NonNull Notification notification) {
        List<Task<Void>> tasks = new ArrayList<>();
        for (String email : userEmails) {
            Notification copy = new Notification(notification.getTitle(), notification.getMessage(), notification.getEventId(), notification.getFromOrganizeremail(), notification.getEventName(), notification.getGroupType());
            tasks.add(sendToUser(email, copy));
        }
        // return wrapper task that completes when all children complete
        return Tasks.whenAll(tasks).continueWith(t -> tasks);
    }

    // helper: log notification for admin reviews
    /**
     * Logs a notification activity for administrative review and auditing purposes.
     * Creates a document in the notification_logs collection regardless of delivery status.
     *
     * @param recipientEmail The email address of the intended recipient
     * @param notification The notification that was attempted to be sent
     * @param status The delivery status ("SENT", "OPTED_OUT", "FAILED")
     * @return A Task that completes when the log entry is written to Firestore
     * @see FirebaseFirestore
     */
    public Task<Void> logNotification(@NonNull String recipientEmail, @NonNull Notification notification, @NonNull String status) {
        Map<String,Object> log = new HashMap<>();
        log.put("recipient", recipientEmail);
        log.put("title", notification.getTitle());
        log.put("message", notification.getMessage());
        log.put("eventId", notification.getEventId());
        log.put("fromOrganizer", notification.getFromOrganizeremail());
        log.put("status", status); // "SENT", "OPTED_OUT", "FAILED"
        log.put("createdAt", FieldValue.serverTimestamp());
        log.put("groupType", notification.getGroupType());
        log.put("eventName", notification.getEventName());

        return db.collection("notification_logs").document().set(log);
    }

    // Organizer convenience methods (these gather emails from event lists then call sendToUsers)
    // send to waitlist
    /**
     * Sends a notification to all users on an event's waitlist.
     * Extracts emails from the waitlist and sends parallel notifications.
     *
     * @param event The event containing the waitlist
     * @param title The title of the notification
     * @param message The message content of the notification
     * @return A Task containing a list of individual send tasks
     * @throws NullPointerException if event, title, or message is null
     * @see #sendToUsers(List, Notification)
     * @see #extractEmailsFromEntrantList(EntrantList)
     */
    public Task<List<Task<Void>>> sendToWaitlist(@NonNull Event event, @NonNull String title, @NonNull String message) {
        List<String> emails = extractEmailsFromEntrantList(event.getWaitlist());
        String groupType = "Waiting List";
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType);
        return sendToUsers(emails, notif);
    }
    /**
     * Sends a notification to all users on an event's invite list.
     * Extracts emails from the invite list and sends parallel notifications.
     *
     * @param event The event containing the invite list
     * @param title The title of the notification
     * @param message The message content of the notification
     * @return A Task containing a list of individual send tasks
     * @throws NullPointerException if event, title, or message is null
     * @see #sendToUsers(List, Notification)
     * @see #extractEmailsFromEntrantList(EntrantList)
     */
    public Task<List<Task<Void>>> sendToInvited(@NonNull Event event, @NonNull String title, @NonNull String message) {
        List<String> emails = extractEmailsFromEntrantList(event.getInviteList());
        String groupType = "Chosen Entrants";
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType);
        return sendToUsers(emails, notif);
    }
    /**
     * Sends a notification to all users on an event's declined/cancelled list.
     * Extracts emails from the declined list and sends parallel notifications.
     *
     * @param event The event containing the declined list
     * @param title The title of the notification
     * @param message The message content of the notification
     * @return A Task containing a list of individual send tasks
     * @throws NullPointerException if event, title, or message is null
     * @see #sendToUsers(List, Notification)
     * @see #extractEmailsFromEntrantList(EntrantList)
     */
    public Task<List<Task<Void>>> sendToCancelled(@NonNull Event event, @NonNull String title, @NonNull String message) {
        List<String> emails = extractEmailsFromEntrantList(event.getDeclinedList());
        String groupType = "Cancelled Entrants";
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType);
        return sendToUsers(emails, notif);
    }

    // small utility to get emails out of your EntrantList
    /**
     * Extracts email addresses from an EntrantList.
     * Utility method that iterates through entrants and collects non-null email addresses.
     *
     * @param list The EntrantList to extract emails from
     * @return A list of email addresses, empty if the input is null or empty
     * @see EntrantList
     * @see Entrant
     */
    private List<String> extractEmailsFromEntrantList(EntrantList list) {
        List<String> out = new ArrayList<>();
        if (list == null || list.size() == 0) return out;
        for (int i = 0; i< list.size(); i++){
            Entrant e = list.getEntrant(i);
            if (e != null && e.getEmail() != null)
                out.add(e.getEmail());
        }
        return out;
    }
    // for admin to review logs
    /**
     * Retrieves notification logs for administrative review.
     * Fetches logs from Firestore ordered by creation timestamp (newest first).
     *
     * @param callback The callback to handle the results or failures
     * @throws NullPointerException if callback is null
     * @see NotificationLogsCallback
     * @see FirebaseFirestore
     */
    public void getNotificationLogs(@NonNull NotificationLogsCallback callback) {
        db.collection("notification_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Map<String,Object>> logs = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        logs.add(doc.getData());
                    }
                    callback.onSuccess(logs);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }
    /**
     * Callback interface for handling notification logs retrieval results.
     *
     * @see #getNotificationLogs(NotificationLogsCallback)
     */
    public interface NotificationLogsCallback {
        /**
         * Callback interface for handling notification logs retrieval results.
         *
         * @see #getNotificationLogs(NotificationLogsCallback)
         */
        void onSuccess(List<Map<String,Object>> logs);
        /**
         * Called when notification logs retrieval fails.
         *
         * @param e The exception that caused the failure
         */
        void onFailure(Exception e);
    }

}
