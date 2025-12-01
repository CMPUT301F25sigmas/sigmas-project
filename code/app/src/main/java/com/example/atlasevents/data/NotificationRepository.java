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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
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
        return sendToUserInternal(userEmail, notification, true);
    }

    // Internal helper so bulk sends can skip per-recipient logging
    private Task<Void> sendToUserInternal(@NonNull String userEmail, @NonNull Notification notification, boolean logIndividually) {
        DocumentReference userRef = db.collection("users").document(userEmail);

        return userRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            DocumentSnapshot userDoc = task.getResult();
            if (userDoc == null) throw new Exception("User doc missing");
            Boolean enabled = userDoc.getBoolean("notificationsEnabled");
            if (enabled != null && !enabled) {
                // user has opted out; still write log for admin but don't push notification into their subcollection
                if (logIndividually) {
                    return logNotification(userEmail, notification, "OPTED_OUT");
                }
                return Tasks.forResult(null);
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
            data.put("recipientCount", notification.getRecipientCount());


            // perform write and then log
            return notifDoc.set(data).continueWithTask(setTask -> {
                if (!setTask.isSuccessful()) throw setTask.getException();
                if (logIndividually) {
                    return logNotification(userEmail, notification, "SENT");
                }
                return Tasks.forResult(null);
            });
        }).addOnFailureListener(e -> Log.w(TAG, "sendToUser failure", e));
    }

    // send to multiple users (fire off parallel tasks)
    /**
     * Sends a notification to multiple users in parallel.
     * I stash the recipient count on the template then fan out copies so each write stays independent.
     *
     * @param userEmails List of email addresses to send the notification to
     * @param notification The notification object to send (will be copied for each user)
     * @return A Task containing a list of individual send tasks that complete when all notifications are processed
     * @see #sendToUser(String, Notification)
     * @see Tasks#whenAll(java.util.Collection)
     */
    public Task<List<Task<Void>>> sendToUsers(@NonNull List<String> userEmails, @NonNull Notification notification) {
        List<Task<Void>> tasks = new ArrayList<>();
        notification.setRecipientCount(userEmails.size()); // Set recipient count
        for (String email : userEmails) {
            Notification copy = new Notification(notification.getTitle(), notification.getMessage(), notification.getEventId(), notification.getFromOrganizeremail(), notification.getEventName(), notification.getGroupType(), notification.getRecipientCount());
            tasks.add(sendToUserInternal(email, copy, false));
        }
        return Tasks.whenAll(tasks).continueWithTask(t -> {
            String organizerEmail = notification.getFromOrganizeremail();
            String status = t.isSuccessful() ? "SENT" : "FAILED";
            return logBatchNotification(organizerEmail, notification, userEmails, status)
                    .continueWith(logTask -> {
                        if (!logTask.isSuccessful()) {
                            throw logTask.getException();
                        }
                        if (!t.isSuccessful()) {
                            throw t.getException();
                        }
                        return tasks;
                    });
        });
    }

    // helper: log notification for admin reviews
    /**
     * Logs a notification activity for administrative review and auditing purposes.
     * I always write a log, even if the user opted out, so admins have traceability.
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
        log.put("recipientCount", notification.getRecipientCount());

        String organizerEmail = notification.getFromOrganizeremail();
        if (organizerEmail == null || organizerEmail.isEmpty()) {
            organizerEmail = "unknown_sender";
        }
        return db.collection("notification_logs")
                .document(organizerEmail)
                .collection("logs")
                .document()
                .set(log);
    }

    // aggregate log for bulk sends so organizer history shows one entry
    private Task<Void> logBatchNotification(String organizerEmail, Notification notification, List<String> recipients, String status) {
        Map<String,Object> log = new HashMap<>();
        log.put("recipient", recipients.size() == 1 ? recipients.get(0) : "Batch");
        log.put("recipients", new ArrayList<>(recipients));
        log.put("title", notification.getTitle());
        log.put("message", notification.getMessage());
        log.put("eventId", notification.getEventId());
        log.put("fromOrganizer", organizerEmail != null && !organizerEmail.isEmpty() ? organizerEmail : "unknown_sender");
        log.put("status", status);
        log.put("createdAt", FieldValue.serverTimestamp());
        log.put("groupType", notification.getGroupType());
        log.put("eventName", notification.getEventName());
        log.put("recipientCount", notification.getRecipientCount());

        String organizerDoc = organizerEmail == null || organizerEmail.isEmpty() ? "unknown_sender" : organizerEmail;
        return db.collection("notification_logs")
                .document(organizerDoc)
                .collection("logs")
                .document()
                .set(log);
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
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType, emails.size());
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

//    public Task<Void> sendEventInvitations(List<String> userEmails, Notification invitation) {
//        Log.d(TAG, "Sending event invitations to " + userEmails.size() + " users");
//
//        List<Task<Void>> tasks = new ArrayList<>();
//
//        for (String userEmail : userEmails) {
//            // Bypass opt-out check for event invitations
//            Task<Void> task = sendNotificationToUser(userEmail, invitation, true);
//            tasks.add(task);
//        }
//
//        return Tasks.whenAll(tasks);
//    }

//    /**
//     * Enhanced send notification method with bypass option
//     */
//    private Task<Void> sendNotificationToUser(String userEmail, Notification notification, boolean bypassOptOut) {
//        if (!bypassOptOut) {
//            // Check if user has opted out of notifications from this organizer
//            return isOrganizerBlocked(userEmail, notification.getFromOrganizeremail())
//                    .continueWithTask(isBlockedTask -> {
//                        boolean isBlocked = Boolean.TRUE.equals(isBlockedTask.getResult());
//                        if (isBlocked) {
//                            Log.d(TAG, "User " + userEmail + " has blocked notifications from " + notification.getFromOrganizeremail());
//                            return Tasks.forResult(null); // Skip sending
//                        }
//                        return storeNotificationForUser(userEmail, notification);
//                    });
//        } else {
//            // Bypass opt-out check (for event invitations)
//            Log.d(TAG, "Bypassing opt-out for event invitation to: " + userEmail);
//            return storeNotificationForUser(userEmail, notification);
//        }
//    }
    public Task<List<Task<Void>>> sendToInvited(@NonNull Event event, @NonNull String title, @NonNull String message) {
        List<String> emails = extractEmailsFromEntrantList(event.getInviteList());
        String groupType = "Chosen Entrants";
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType, emails.size());
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
        Notification notif = new Notification(title, message, event.getId(), event.getOrganizer().getEmail(), event.getEventName(), groupType, emails.size());
        return sendToUsers(emails, notif);
    }

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

    public void getNotificationLogs(@NonNull NotificationLogsCallback callback) {
        Log.d(TAG, "Fetching ALL notification logs with index fallback");

        // Try the indexed query first
        db.collectionGroup("logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    Log.d(TAG, "Indexed query successful. Found " + qs.size() + " documents");

                    List<Map<String,Object>> logs = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        logs.add(doc.getData());
                    }
                    callback.onSuccess(logs);
                })
                .addOnFailureListener(e -> {
                    // Check if it's an index error
                    if (e.getMessage() != null && e.getMessage().contains("index")) {
                        Log.w(TAG, "Index missing, using fallback method");
                        // Use the index-safe version as fallback
                        getNotificationLogsIndexSafe(callback);
                    } else {
                        Log.e(TAG, "Error fetching notification logs: " + e.getMessage(), e);
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Index-safe version that queries each organizer separately
     */
    private void getNotificationLogsIndexSafe(@NonNull NotificationLogsCallback callback) {
        Log.d(TAG, "Fetching ALL notification logs (index-safe version)");

        // Get all organizer documents first
        db.collection("notification_logs")
                .get()
                .addOnSuccessListener(organizerSnapshot -> {
                    Log.d(TAG, "Found " + organizerSnapshot.size() + " organizer documents");

                    if (organizerSnapshot.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    // Create a list to hold all log data
                    List<Map<String, Object>> allLogs = new ArrayList<>();
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                    // Query each organizer's logs subcollection
                    for (DocumentSnapshot organizerDoc : organizerSnapshot.getDocuments()) {
                        String organizerEmail = organizerDoc.getId();

                        // Query WITHOUT orderBy to avoid index requirement
                        Task<QuerySnapshot> task = organizerDoc.getReference()
                                .collection("logs")
                                .get()  // No orderBy - this avoids the index requirement
                                .addOnSuccessListener(logsSnapshot -> {
                                    Log.d(TAG, "Found " + logsSnapshot.size() + " logs for organizer: " + organizerEmail);

                                    for (DocumentSnapshot logDoc : logsSnapshot.getDocuments()) {
                                        Map<String, Object> logData = logDoc.getData();
                                        if (logData != null) {
                                            // Add organizer email for reference
                                            logData.put("_organizerEmail", organizerEmail);
                                            allLogs.add(logData);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching logs for organizer: " + organizerEmail, e);
                                });

                        tasks.add(task);
                    }

                    // Wait for all queries to complete
                    Tasks.whenAllComplete(tasks)
                            .addOnSuccessListener(allTasks -> {
                                // Sort by createdAt manually (descending)
                                allLogs.sort((a, b) -> {
                                    try {
                                        Object dateA = a.get("createdAt");
                                        Object dateB = b.get("createdAt");

                                        if (dateA == null && dateB == null) return 0;
                                        if (dateA == null) return 1; // nulls last
                                        if (dateB == null) return -1; // nulls last

                                        // Handle Firebase Timestamp and Date objects
                                        long timeA = convertToMillis(dateA);
                                        long timeB = convertToMillis(dateB);

                                        return Long.compare(timeB, timeA); // Descending order
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error sorting logs", e);
                                        return 0;
                                    }
                                });

                                Log.d(TAG, "Total logs collected: " + allLogs.size());
                                callback.onSuccess(allLogs);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error collecting logs from all organizers", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching organizer documents", e);
                    callback.onFailure(e);
                });
    }
    /**
     * Convert Firebase Timestamp or Date to milliseconds
     */
    private long convertToMillis(Object timestamp) {
        if (timestamp == null) {
            return 0;
        }

        if (timestamp instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestamp).toDate().getTime();
        } else if (timestamp instanceof Date) {
            return ((Date) timestamp).getTime();
        } else if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else {
            Log.w(TAG, "Unknown timestamp type: " + timestamp.getClass().getName());
            return 0;
        }
    }

    public interface NotificationLogsCallback {
        void onSuccess(List<Map<String,Object>> logs);
        void onFailure(Exception e);
    }

}
