

package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import com.example.atlasevents.data.model.Notification;
import com.example.atlasevents.data.NotificationRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class NotificationRepositoryTest {

    // --- All your @Mock annotations are correct ---
    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDocRef;
    @Mock private CollectionReference mockNotificationsCollection;
    @Mock private DocumentReference mockNotificationDocRef;
    @Mock private CollectionReference mockLogsCollection;
    @Mock private DocumentReference mockLogDocRef;
    @Mock private DocumentSnapshot mockUserDocSnapshot;
    @Mock private Task<DocumentSnapshot> mockGetUserTask;
    @Mock private Task<Void> mockSetNotificationTask;
    @Mock private Task<Void> mockLogTask;

    private MockedStatic<FirebaseFirestore> mockedFirestore;
    private MockedStatic<Tasks> mockedTasks;
    private NotificationRepository notificationRepository;
    private Notification testNotification;

    @Before
    public void setUp() {
        // --- Static Mocking ---
        mockedFirestore = mockStatic(FirebaseFirestore.class);
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
        mockedTasks = mockStatic(Tasks.class);

        // --- Object Creation ---
        notificationRepository = new NotificationRepository();

        // --- COMMON Mock Hierarchy (used by all tests) ---
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocRef);
        when(mockUserDocRef.get()).thenReturn(mockGetUserTask);

        // Handle the first continuation for ALL tests
        when(mockGetUserTask.continueWithTask(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.Continuation<DocumentSnapshot, Task<Void>> continuation = invocation.getArgument(0);
            return continuation.then(mockGetUserTask);
        });
        when(mockGetUserTask.addOnFailureListener(any())).thenReturn(mockGetUserTask);

        // Common logging setup
        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
        when(mockLogsCollection.document()).thenReturn(mockLogDocRef);
        when(mockLogDocRef.set(anyMap())).thenReturn(mockLogTask);

        // Common notification setup (needed for the "enabled" test)
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.document()).thenReturn(mockNotificationDocRef);
        when(mockNotificationDocRef.set(anyMap())).thenReturn(mockSetNotificationTask);

        testNotification = new Notification("Test Title", "Test Message", "event123", "organizer@test.com", "Test Event", "Test Group");
    }

    @After
    public void tearDown() {
        mockedFirestore.close();
        mockedTasks.close();
    }

    @Test
    public void sendToUser_whenNotificationsEnabled_sendsAndLogsAsSent() {
        // --- Arrange ---
        // 1. Simulate a successful user fetch
        when(mockGetUserTask.isSuccessful()).thenReturn(true);
        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);

        // 2. >>> THE FIX IS HERE <<<
        //    Simulate a SUCCESSFUL inner set task. This prevents the 'getException()' call.
        when(mockSetNotificationTask.isSuccessful()).thenReturn(true);

        // 3. Move the specific continuation stubbing here.
        when(mockSetNotificationTask.continueWithTask(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.Continuation<Void, Task<Void>> continuation = invocation.getArgument(0);
            return continuation.then(mockSetNotificationTask);
        });

        // --- Act ---
        notificationRepository.sendToUser("user@test.com", testNotification);

        // --- Assert ---
        ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockNotificationDocRef).set(anyMap());
        verify(mockLogDocRef).set(logCaptor.capture());
        assertEquals("SENT", logCaptor.getValue().get("status"));
    }

    @Test
    public void sendToUser_whenNotificationsDisabled_onlyLogsAsOptedOut() {
        // --- Arrange ---
        when(mockGetUserTask.isSuccessful()).thenReturn(true);
        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);
        // No other arrangement is needed, as the inner path is not taken.

        // --- Act ---
        notificationRepository.sendToUser("user@test.com", testNotification);

        // --- Assert ---
        ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockNotificationDocRef, never()).set(anyMap());
        verify(mockLogDocRef).set(logCaptor.capture());
        assertEquals("OPTED_OUT", logCaptor.getValue().get("status"));
    }
}

//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.*;
//import com.example.atlasevents.data.model.Notification;
//import com.example.atlasevents.data.NotificationRepository;
//import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.firestore.CollectionReference;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.util.Map;
//
//
//@RunWith(MockitoJUnitRunner.class)
//public class NotificationRepositoryTest {
//
//    // --- Mocks for Firestore Structure ---
//    @Mock private FirebaseFirestore mockDb;
//    @Mock private CollectionReference mockUsersCollection;
//    @Mock private DocumentReference mockUserDocRef;
//    @Mock private CollectionReference mockNotificationsCollection;
//    @Mock private DocumentReference mockNotificationDocRef;
//    @Mock private CollectionReference mockLogsCollection;
//    @Mock private DocumentReference mockLogDocRef;
//
//    // --- Mocks for Firestore Results ---
//    @Mock private DocumentSnapshot mockUserDocSnapshot;
//
//    // --- Mocks for Asynchronous Tasks ---
//    @Mock private Task<DocumentSnapshot> mockGetUserTask;
//    @Mock private Task<Void> mockSetNotificationTask;
//    @Mock private Task<Void> mockLogTask;
//
//    // --- Static Mock Controller ---
//    private MockedStatic<FirebaseFirestore> mockedFirestore;
//    private MockedStatic<Tasks> mockedTasks;
//
//    private NotificationRepository notificationRepository;
//    private Notification testNotification;
//
//    @Before
//    public void setUp() {
//
//        // Hijacking the FirebaseFirestore class BEFORE the repository is created.
//        mockedFirestore = mockStatic(FirebaseFirestore.class);
//        // Whenever getInstance() is called anywhere, return our mock 'db' instead.
//        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
//
//        // Also mock the static 'Tasks' utility class from Google Play Services
//        mockedTasks = mockStatic(Tasks.class);
//
//
//        //The constructor will get our mock 'db'.
//        notificationRepository = new NotificationRepository();
//
//
//        // Mock the chain of calls for sending a notification
//        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
//        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocRef);
//        when(mockUserDocRef.get()).thenReturn(mockGetUserTask);
//        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
//        when(mockNotificationsCollection.document()).thenReturn(mockNotificationDocRef);
//        when(mockNotificationDocRef.set(anyMap())).thenReturn(mockSetNotificationTask);
//
//        // Mock the chain of calls for logging
//        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
//        when(mockLogsCollection.document()).thenReturn(mockLogDocRef);
//        when(mockLogDocRef.set(anyMap())).thenReturn(mockLogTask);
//
//        when(mockGetUserTask.continueWithTask(any())).thenAnswer(invocation -> {
//            // This code simulates the Task API. It gets the lambda passed to continueWithTask
//            com.google.android.gms.tasks.Continuation<DocumentSnapshot, Task<Void>> continuation = invocation.getArgument(0);
//            // and immediately executes it, passing the mockGetUserTask as the argument.
//            return continuation.then(mockGetUserTask);
//        });
//
//        when(mockGetUserTask.addOnFailureListener(any())).thenReturn(mockGetUserTask);
//
//        testNotification = new Notification("Test Title", "Test Message", "event123", "organizer@test.com", "Test Event", "Test Group");
//    }
//
//    @After
//    public void tearDown() {
//        // Release the static mocks after each test to prevent test pollution.
//        mockedFirestore.close();
//        mockedTasks.close();
//    }
//
//    @Test
//    public void sendToUser_whenNotificationsEnabled_sendsAndLogsAsSent() throws Exception {
//        // Simulate a successful user document fetch
//        when(mockGetUserTask.isSuccessful()).thenReturn(true);
//        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
//
//        // Simulate the user having notifications enabled
//        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);
//
//        // Simulate the chained task for logging after sending
//        // When .set() returns a task, and continueWithTask is called on it...
//        when(mockSetNotificationTask.continueWithTask(any())).thenAnswer(invocation -> {
//            com.google.android.gms.tasks.Continuation<Void, Task<Void>> continuation = invocation.getArgument(0);
//            return continuation.then(mockSetNotificationTask);
//        });
//
//        // --- Act ---
//        // Call the method under test. We use Tasks.await to handle the async nature in a test.
//        Tasks.await(notificationRepository.sendToUser("user@test.com", testNotification));
//
//        // --- Assert ---
//        ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
//
//        // Verify that we tried to set the notification in the user's sub-collection
//        verify(mockNotificationDocRef).set(anyMap());
//        // Verify that we also tried to create a log document
//        verify(mockLogDocRef).set(logCaptor.capture());
//
//        // Check the content of the log to ensure the status was "SENT"
//        assertEquals("SENT", logCaptor.getValue().get("status"));
//    }
//
//    @Test
//    public void sendToUser_whenNotificationsDisabled_onlyLogsAsOptedOut() throws Exception {
//        // --- Arrange ---
//        // Simulate a successful user document fetch
//        when(mockGetUserTask.isSuccessful()).thenReturn(true);
//        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
//
//        // Simulate the user having notifications DISABLED
//        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);
//
//        // --- Act ---
//        Tasks.await(notificationRepository.sendToUser("user@test.com", testNotification));
//
//        // --- Assert ---
//        ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
//
//        // Verify that we NEVER tried to set a notification in the user's sub-collection
//        verify(mockNotificationDocRef, never()).set(anyMap());
//        // Verify that we DID create a log document
//        verify(mockLogDocRef).set(logCaptor.capture());
//
//        // Check the content of the log to ensure the status was "OPTED_OUT"
//        assertEquals("OPTED_OUT", logCaptor.getValue().get("status"));
//    }
//}
