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
        lenient().when(mockGetUserTask.addOnFailureListener(any())).thenReturn(mockGetUserTask);

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

