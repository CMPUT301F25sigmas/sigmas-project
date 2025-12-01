package com.example.atlasevents;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class NotificationRepositoryTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDocRef;
    @Mock private CollectionReference mockNotificationsCollection;
    @Mock private DocumentReference mockNotificationDocRef;
    @Mock private CollectionReference mockLogsCollection;
    @Mock private DocumentReference mockLogDocRef;
    @Mock private DocumentReference mockOrganizerLogDocRef; // NEW: For organizer-specific log document
    @Mock private CollectionReference mockOrganizerLogsCollection; // NEW: For organizer's logs subcollection
    @Mock private DocumentSnapshot mockUserDocSnapshot;
    @Mock private Task<DocumentSnapshot> mockGetUserTask;
    @Mock private Task<Void> mockSetNotificationTask;
    @Mock private Task<Void> mockLogTask;
    @Mock private Task<Void> mockOrganizerLogTask; // NEW: For organizer log task

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


        // 1. Users collection chain
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocRef);
        when(mockUserDocRef.get()).thenReturn(mockGetUserTask);
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.document()).thenReturn(mockNotificationDocRef);
        when(mockNotificationDocRef.set(anyMap())).thenReturn(mockSetNotificationTask);

        // 2. Notification logs collection chain - FIXED THIS PART
        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);

        // For general log document (used in batch logging or when organizerEmail is unknown)
        Mockito.lenient().when(mockLogsCollection.document()).thenReturn(mockLogDocRef);
        Mockito.lenient().when(mockLogDocRef.set(anyMap())).thenReturn(mockLogTask);

        // For organizer-specific log document (used in logNotification method)
        // organizerEmail = "organizer@test.com" from testNotification
        Mockito.lenient().when(mockLogsCollection.document("organizer@test.com")).thenReturn(mockOrganizerLogDocRef);
        Mockito.lenient().when(mockOrganizerLogDocRef.collection("logs")).thenReturn(mockOrganizerLogsCollection);

        // Create a mock document reference for the logs subcollection
        DocumentReference mockLogDocumentRef = mock(DocumentReference.class);
        Mockito.lenient().when(mockOrganizerLogsCollection.document()).thenReturn(mockLogDocumentRef);
        Mockito.lenient().when(mockLogDocumentRef.set(anyMap())).thenReturn(mockOrganizerLogTask);

        // Handle task continuations
        when(mockGetUserTask.continueWithTask(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.Continuation<DocumentSnapshot, Task<Void>> continuation = invocation.getArgument(0);
            return continuation.then(mockGetUserTask);
        });

        lenient().when(mockGetUserTask.addOnFailureListener(any())).thenReturn(mockGetUserTask);
        lenient().when(mockSetNotificationTask.addOnFailureListener(any())).thenReturn(mockSetNotificationTask);
        lenient().when(mockLogTask.addOnFailureListener(any())).thenReturn(mockLogTask);
        lenient().when(mockOrganizerLogTask.addOnFailureListener(any())).thenReturn(mockOrganizerLogTask);

        // Setup test notification
        testNotification = new Notification("Test Title", "Test Message", "event123", "organizer@test.com", "Test Event", "Test Group");
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
        if (mockedTasks != null) {
            mockedTasks.close();
        }
    }

    @Test
    public void sendToUser_whenNotificationsEnabled_sendsAndLogsAsSent() {
        // --- Arrange ---
        // 1. Simulate a successful user fetch
        when(mockGetUserTask.isSuccessful()).thenReturn(true);
        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);

        // 2. Simulate a SUCCESSFUL inner set task
        when(mockSetNotificationTask.isSuccessful()).thenReturn(true);

        // 3. Setup continuation for set task
        when(mockSetNotificationTask.continueWithTask(any())).thenAnswer(invocation -> {
            com.google.android.gms.tasks.Continuation<Void, Task<Void>> continuation = invocation.getArgument(0);
            return continuation.then(mockSetNotificationTask);
        });

        // 4. Setup organizer log task
        Mockito.lenient().when(mockOrganizerLogTask.isSuccessful()).thenReturn(true);

        // --- Act ---
        notificationRepository.sendToUser("user@test.com", testNotification);

        // --- Assert ---
        // Verify notification was sent
        verify(mockNotificationDocRef).set(anyMap());

        // Verify log was created in organizer's logs subcollection
        verify(mockOrganizerLogsCollection).document(); // Should create a document in logs subcollection
    }

    @Test
    public void sendToUser_whenNotificationsDisabled_onlyLogsAsOptedOut() {
        // --- Arrange ---
        when(mockGetUserTask.isSuccessful()).thenReturn(true);
        when(mockGetUserTask.getResult()).thenReturn(mockUserDocSnapshot);
        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);

        // Setup organizer log task for OPTED_OUT
        Mockito.lenient().when(mockOrganizerLogTask.isSuccessful()).thenReturn(true);

        // --- Act ---
        notificationRepository.sendToUser("user@test.com", testNotification);

        // --- Assert ---
        // Verify notification was NOT sent
        verify(mockNotificationDocRef, never()).set(anyMap());

        // Verify log was created in organizer's logs subcollection for OPTED_OUT
        verify(mockOrganizerLogsCollection).document(); // Should still log as OPTED_OUT
    }

    @Test
    public void testLogNotification_CreatesLogInOrganizerSubcollection() {
        // Test the logNotification method directly
        String recipientEmail = "recipient@test.com";
        String organizerEmail = "organizer@test.com";
        String status = "SENT";

        Notification testNotif = new Notification("Test", "Message", "event1", organizerEmail, "Event", "Group");

        // Setup organizer-specific chain for this test
        DocumentReference mockOrganizerDocRef = mock(DocumentReference.class);
        CollectionReference mockLogsCollection = mock(CollectionReference.class);
        DocumentReference mockLogDocRef = mock(DocumentReference.class);
        Task<Void> mockTask = mock(Task.class);

        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
        when(mockLogsCollection.document(organizerEmail)).thenReturn(mockOrganizerDocRef);
        when(mockOrganizerDocRef.collection("logs")).thenReturn(mockLogsCollection); // Reuse mock
        when(mockLogsCollection.document()).thenReturn(mockLogDocRef);
        when(mockLogDocRef.set(anyMap())).thenReturn(mockTask);

        // Act
        Task<Void> result = notificationRepository.logNotification(recipientEmail, testNotif, status);

        // Assert
        assertNotNull(result);
        verify(mockLogsCollection).document(organizerEmail);
        verify(mockOrganizerDocRef).collection("logs");
        verify(mockLogDocRef).set(anyMap());
    }

    @Test
    public void testLogNotification_WithUnknownOrganizer_UsesDefault() {
        // Test when organizer email is null or empty
        String recipientEmail = "recipient@test.com";
        String status = "SENT";

        // Create notification with null organizer email
        Notification testNotif = new Notification("Test", "Message", "event1", "", "Event", "Group");

        // Setup default organizer chain
        when(mockLogsCollection.document("unknown_sender")).thenReturn(mockOrganizerLogDocRef);
        when(mockOrganizerLogDocRef.collection("logs")).thenReturn(mockOrganizerLogsCollection);

        // Act
        notificationRepository.logNotification(recipientEmail, testNotif, status);

        // Assert - Should use "unknown_sender" as document ID
        verify(mockLogsCollection).document("unknown_sender");
    }
}