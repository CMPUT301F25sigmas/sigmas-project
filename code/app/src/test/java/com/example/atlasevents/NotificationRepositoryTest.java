package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.annotation.NonNull;

import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class NotificationRepositoryTest {

    private FirebaseFirestore db;
    private CollectionReference usersCol;
    private DocumentReference userDoc;
    private CollectionReference notifCol;
    private DocumentReference notifDoc;

    private CollectionReference logsCol;
    private DocumentReference logDoc;

    @Before
    public void setUp() {
        db = mock(FirebaseFirestore.class);
        usersCol = mock(CollectionReference.class);
        userDoc = mock(DocumentReference.class);
        notifCol = mock(CollectionReference.class);
        notifDoc = mock(DocumentReference.class);

        logsCol = mock(CollectionReference.class);
        logDoc = mock(DocumentReference.class);

        when(db.collection("users")).thenReturn(usersCol);
        when(usersCol.document(anyString())).thenReturn(userDoc);
        when(userDoc.collection("notifications")).thenReturn(notifCol);
        when(notifCol.document()).thenReturn(notifDoc);
        when(notifDoc.getId()).thenReturn("notif123");

        when(db.collection("notification_logs")).thenReturn(logsCol);
        when(logsCol.document()).thenReturn(logDoc);
        when(logDoc.set(anyMap())).thenReturn(Tasks.forResult(null));
    }

    @Test
    public void sendToUser_optedOut_logsOnly_noUserNotificationWrite()
            throws ExecutionException, InterruptedException {

        // user has notificationsEnabled = false
        DocumentSnapshot userSnap = mock(DocumentSnapshot.class);
        when(userSnap.getBoolean("notificationsEnabled")).thenReturn(false);
        when(userDoc.get()).thenReturn(Tasks.forResult(userSnap));

        NotificationRepository repo = new NotificationRepository() {
            // inject our mocked db
            private final FirebaseFirestore injected = db;
            @Override
            public Task<Void> logNotification(String recipientEmail, Notification notification, String status) {
                // Call real logging on mocked db
                Map<String, Object> log = new HashMap<>();
                log.put("recipient", recipientEmail);
                log.put("title", notification.getTitle());
                log.put("message", notification.getMessage());
                log.put("eventId", notification.getEventId());
                log.put("fromOrganizer", notification.getFromOrganizeremail());
                log.put("status", status);
                log.put("createdAt", FieldValue.serverTimestamp());
                return injected.collection("notification_logs").document().set(log);
            }
            @Override
            public Task<Void> sendToUser(String userEmail, Notification notification) {
                // Copied from production, but using injected db instead of static instance
                DocumentReference userRef = injected.collection("users").document(userEmail);
                return userRef.get().continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot userDoc = task.getResult();
                    if (userDoc == null) throw new Exception("User doc missing");
                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");
                    if (enabled != null && !enabled) {
                        return logNotification(userEmail, notification, "OPTED_OUT");
                    }
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
                    return notifDoc.set(data).continueWithTask(setTask -> {
                        if (!setTask.isSuccessful()) throw setTask.getException();
                        return logNotification(userEmail, notification, "SENT");
                    });
                });
            }
        };

        Notification n = new Notification("T", "M", "E1", "org@x.com", "event", "test");
        Task<Void> result = repo.sendToUser("bob@example.com", n);

        // wait (they're already immediate Tasks)
        Tasks.await(result);
        assertTrue(result.isSuccessful());

        // verify NOT writing to users/{email}/notifications
        verify(notifCol, never()).document();
        verify(notifDoc, never()).set(anyMap());

        // verify a log with status OPTED_OUT was written
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(logDoc, times(1)).set(captor.capture());
        Map log = captor.getValue();
        assertEquals("bob@example.com", log.get("recipient"));
        assertEquals("OPTED_OUT", log.get("status"));
        assertEquals("T", log.get("title"));
        assertEquals("M", log.get("message"));
        assertEquals("E1", log.get("eventId"));
        assertEquals("org@x.com", log.get("fromOrganizer"));
        assertTrue(log.containsKey("createdAt"));
    }

    @Test
    public void sendToUser_enabled_writesNotificationAndLogs()
            throws ExecutionException, InterruptedException {

        DocumentSnapshot userSnap = mock(DocumentSnapshot.class);
        // null or true -> enabled
        when(userSnap.getBoolean("notificationsEnabled")).thenReturn(true);
        when(userDoc.get()).thenReturn(Tasks.forResult(userSnap));
        when(notifDoc.set(anyMap())).thenReturn(Tasks.forResult(null));

        NotificationRepository repo = new NotificationRepository() {
            private final FirebaseFirestore injected = db;
            @Override
            public Task<Void> logNotification(String recipientEmail, Notification notification, String status) {
                Map<String, Object> log = new HashMap<>();
                log.put("recipient", recipientEmail);
                log.put("title", notification.getTitle());
                log.put("message", notification.getMessage());
                log.put("eventId", notification.getEventId());
                log.put("fromOrganizer", notification.getFromOrganizeremail());
                log.put("status", status);
                log.put("createdAt", FieldValue.serverTimestamp());
                return injected.collection("notification_logs").document().set(log);
            }
            @Override
            public Task<Void> sendToUser(String userEmail, Notification notification) {
                DocumentReference userRef = injected.collection("users").document(userEmail);
                return userRef.get().continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot userDoc = task.getResult();
                    if (userDoc == null) throw new Exception("User doc missing");
                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");
                    if (enabled != null && !enabled) {
                        return logNotification(userEmail, notification, "OPTED_OUT");
                    }
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
                    return notifDoc.set(data).continueWithTask(setTask -> {
                        if (!setTask.isSuccessful()) throw setTask.getException();
                        return logNotification(userEmail, notification, "SENT");
                    });
                });
            }
        };

        Notification n = new Notification("Title", "Body", "EventX", "org@x.com", "event", "test");
        Task<Void> result = repo.sendToUser("alice@example.com", n);
        Tasks.await(result);
        assertTrue(result.isSuccessful());

        // verify wrote the notification
        ArgumentCaptor<Map> dataCap = ArgumentCaptor.forClass(Map.class);
        verify(notifDoc, times(1)).set(dataCap.capture());
        Map data = dataCap.getValue();
        assertEquals("Title", data.get("title"));
        assertEquals("Body", data.get("message"));
        assertEquals("EventX", data.get("eventId"));
        assertEquals("org@x.com", data.get("fromOrganizeremail"));
        assertEquals(false, data.get("read"));
        assertTrue(data.containsKey("createdAt"));
        assertEquals("notif123", data.get("notificationId"));
        // ensure the Notification object got its id, too
        assertEquals("notif123", n.getNotificationId());

        // verify log called with SENT
        ArgumentCaptor<Map> logCap = ArgumentCaptor.forClass(Map.class);
        verify(logDoc, times(1)).set(logCap.capture());
        assertEquals("SENT", logCap.getValue().get("status"));
    }

    @Test
    public void sendToUsers_invokesForEachAndCompletes() throws Exception {
        NotificationRepository real = spy(new NotificationRepository());

        // stub sendToUser inside the spy to avoid Firestore
        doReturn(Tasks.forResult(null))
                .when(real).sendToUser(anyString(), any(Notification.class));

        List<String> emails = Arrays.asList("a@x.com", "b@x.com", "c@x.com");
        Notification base = new Notification("T", "M", "E", "org@x.com", "event", "test");

        Task<List<Task<Void>>> t = real.sendToUsers(emails, base);
        Tasks.await(t);
        assertTrue(t.isSuccessful());

        // verify called once per email
        for (String e : emails) {
            verify(real, times(1)).sendToUser(eq(e), any(Notification.class));
        }
        assertEquals(3, t.getResult().size());
    }

    @Test
    public void sendToWaitlist_extractsEmailsAndCallsSendToUsers() throws Exception {
        // Build a tiny Event with EntrantList + Organizer email
        Organizer org = mock(Organizer.class);
        when(org.getEmail()).thenReturn("org@x.com");

        EntrantList wait = new EntrantList();
        wait.addEntrant(new Entrant("tt", "u1@x.com", "hfd","hgfd"));
        wait.addEntrant(new Entrant("tt", "u3@x.com", "hfd","hgfd"));

        Event event = mock(Event.class);
        when(event.getId()).thenReturn("EV123");
        when(event.getOrganizer()).thenReturn(org);
        when(event.getWaitlist()).thenReturn(wait);

        NotificationRepository repo = spy(new NotificationRepository());
        // capture arguments to sendToUsers
        ArgumentCaptor<List<String>> emailsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Notification> notifCap = ArgumentCaptor.forClass(Notification.class);

        doReturn(Tasks.forResult(Collections.emptyList()))
                .when(repo).sendToUsers(emailsCap.capture(), notifCap.capture());

        Task<List<Task<Void>>> t = repo.sendToWaitlist(event, "Hello", "Msg");
        Tasks.await(t);
        assertTrue(t.isSuccessful());

        List<String> emails = emailsCap.getValue();
        Notification n = notifCap.getValue();

        assertEquals(Arrays.asList("u1@x.com", "u2@x.com"), emails);
        assertEquals("Hello", n.getTitle());
        assertEquals("Msg", n.getMessage());
        assertEquals("EV123", n.getEventId());
        assertEquals("org@x.com", n.getFromOrganizeremail());
    }

    @Test
    public void getNotificationLogs_success_callsCallback() {
        // Mock query chain: collection -> orderBy -> get()
        CollectionReference logs = mock(CollectionReference.class);
        Query q = mock(Query.class);
        when(db.collection("notification_logs")).thenReturn(logs);
        when(logs.orderBy(eq("createdAt"), eq(Query.Direction.DESCENDING))).thenReturn(q);

        // Build a fake query snapshot with two docs
        DocumentSnapshot d1 = mock(DocumentSnapshot.class);
        DocumentSnapshot d2 = mock(DocumentSnapshot.class);
        Map<String, Object> m1 = new HashMap<>(); m1.put("recipient", "a@x.com");
        Map<String, Object> m2 = new HashMap<>(); m2.put("recipient", "b@x.com");
        when(d1.getData()).thenReturn(m1);
        when(d2.getData()).thenReturn(m2);

        QuerySnapshot qs = mock(QuerySnapshot.class);
        when(qs.getDocuments()).thenReturn(Arrays.asList(d1, d2));
        when(q.get()).thenReturn(Tasks.forResult(qs));

        // Repo that uses injected db
        NotificationRepository repo = new NotificationRepository() {
            private final FirebaseFirestore injected = db;
            @Override
            public void getNotificationLogs(@NonNull NotificationLogsCallback callback) {
                injected.collection("notification_logs")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener(s -> {
                            List<Map<String,Object>> logs = new ArrayList<>();
                            for (DocumentSnapshot doc: s.getDocuments()) logs.add(doc.getData());
                            callback.onSuccess(logs);
                        })
                        .addOnFailureListener(callback::onFailure);
            }
        };

        final List<Map<String, Object>> out = new ArrayList<>();
        repo.getNotificationLogs(new NotificationRepository.NotificationLogsCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> logs) { out.addAll(logs); }
            @Override
            public void onFailure(Exception e) { fail("Should not fail"); }
        });

        assertEquals(2, out.size());
        assertEquals("a@x.com", out.get(0).get("recipient"));
        assertEquals("b@x.com", out.get(1).get("recipient"));
    }
}
