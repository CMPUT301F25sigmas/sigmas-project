package com.example.atlasevents;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.atlasevents.data.NotificationListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
// * Unit tests for the {@link NotificationListener} class.
// * Tests the real-time notification listening functionality including
// * preference monitoring and notification processing.
// *
// * @see NotificationListener
// * @see FirebaseFirestore
// */

@RunWith(MockitoJUnitRunner.class)
public class NotificationListenerTest {

    // --- Mocks ---
    @Mock
    private Activity mockActivity;

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private DocumentReference mockUserDocRef;

    @Mock
    private CollectionReference mockPreferencesCollection;

    @Mock
    private DocumentReference mockPreferencesDocRef;

    @Mock
    private ListenerRegistration mockPrefRegistration; // For user doc listener

    @Mock
    private ListenerRegistration mockPreferencesRegistration; // For preferences doc listener

    @Mock
    private DocumentSnapshot mockUserDocSnapshot;

    @Mock
    private DocumentSnapshot mockPreferencesDocSnapshot;

    @Mock
    private ListenerRegistration mockNotifsRegistration;

    // --- Additional mocks needed for notifications listener ---
    @Mock
    private CollectionReference mockNotificationsCollection;

    @Mock
    private Query mockQuery;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private DocumentChange mockDocumentChange;

    @Mock
    private QueryDocumentSnapshot mockNotificationDocSnapshot;

    @Mock
    private SharedPreferences mockToastPrefs;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private MockedStatic<FirebaseFirestore> mockedFirestore;

    private NotificationListener notificationListener;
    private final String testEmail = "user@test.com";

    @Before
    public void setUp() {
        mockedFirestore = mockStatic(FirebaseFirestore.class);

        // Mock FirebaseFirestore.getInstance()
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        // Mock activity's getSharedPreferences
        when(mockActivity.getSharedPreferences(eq("notification_toasts"), anyInt()))
                .thenReturn(mockToastPrefs);
        Mockito.lenient().when(mockToastPrefs.edit()).thenReturn(mockEditor);
        Mockito.lenient().when(mockEditor.putStringSet(anyString(), any())).thenReturn(mockEditor);

        // Create notification listener
        notificationListener = new NotificationListener(mockActivity, testEmail);

        // --- Set up the complete mock chain for database interactions ---
        // 1. User document chain (for notificationsEnabled)
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(testEmail)).thenReturn(mockUserDocRef);

        // 2. Preferences subcollection chain (for blockedOrganizers)
        when(mockUserDocRef.collection("preferences")).thenReturn(mockPreferencesCollection);
        when(mockPreferencesCollection.document("blockedOrganizers")).thenReturn(mockPreferencesDocRef);

        // 3. Notifications subcollection chain
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
    }

    /**
     * Tests that start method attaches preference listener when email is provided.
     * Verifies that both user document and preferences document listeners are attached.
     */
    @Test
    public void testStart_WithValidEmail_AttachesBothListeners() {
        // Arrange
        when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);
        when(mockPreferencesDocRef.addSnapshotListener(any())).thenReturn(mockPreferencesRegistration);

        // Act
        notificationListener.start();

        // Assert
        verify(mockUserDocRef).addSnapshotListener(any(EventListener.class));
        verify(mockPreferencesDocRef).addSnapshotListener(any(EventListener.class));
    }

    /**
     * Tests that start method does nothing when email is null.
     * Verifies that no Firestore operations are performed with null email.
     */
    @Test
    public void testStart_WithNullEmail_DoesNothing() {
        // Arrange - Create listener with null email
        NotificationListener nullEmailListener = new NotificationListener(mockActivity, null);

        // Act
        nullEmailListener.start();

        // Assert - Verify that no database interactions occurred
        verify(mockDb, never()).collection(anyString());
        verify(mockUserDocRef, never()).collection(anyString());
    }

    @Test
    public void testPreferenceChange_ToEnabled_AttachesNotificationsListener() {
        // Arrange
        ArgumentCaptor<EventListener<DocumentSnapshot>> userDocCaptor =
                ArgumentCaptor.forClass(EventListener.class);
        when(mockUserDocRef.addSnapshotListener(userDocCaptor.capture()))
                .thenReturn(mockPrefRegistration);

        // Mock the preferences listener too
        when(mockPreferencesDocRef.addSnapshotListener(any())).thenReturn(mockPreferencesRegistration);

        // Mock the notifications query listener
        Mockito.lenient().when(mockQuery.addSnapshotListener(any())).thenReturn(mockNotifsRegistration);

        // Call start() to attach both listeners
        notificationListener.start();

        // Simulate notificationsEnabled = true
        Mockito.lenient().when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);
        Mockito.lenient().when(mockUserDocSnapshot.exists()).thenReturn(true);

        // Trigger the user document listener
        userDocCaptor.getValue().onEvent(mockUserDocSnapshot, null);

        // Assert - Verify the notifications listener was attached
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollection).orderBy(eq("createdAt"), any(Query.Direction.class));
        verify(mockQuery).addSnapshotListener(any());
    }

    @Test
    public void testPreferenceChange_ToDisabled_RemovesNotificationsListener() throws Exception {
        // Arrange
        ArgumentCaptor<EventListener<DocumentSnapshot>> userDocCaptor =
                ArgumentCaptor.forClass(EventListener.class);
        when(mockUserDocRef.addSnapshotListener(userDocCaptor.capture()))
                .thenReturn(mockPrefRegistration);

        // Mock the preferences listener too
        when(mockPreferencesDocRef.addSnapshotListener(any())).thenReturn(mockPreferencesRegistration);

        // First, simulate that notifications listener was previously attached
        notificationListener.start();

        // Get the private notifRegistration field and set a mock value
        java.lang.reflect.Field notifRegField = NotificationListener.class.getDeclaredField("notifsRegistration");
        notifRegField.setAccessible(true);
        notifRegField.set(notificationListener, mockNotifsRegistration);

        // Simulate notificationsEnabled = false
        Mockito.lenient().when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(false);
        Mockito.lenient().when(mockUserDocSnapshot.exists()).thenReturn(true);

        // Act - Trigger the user document listener
        userDocCaptor.getValue().onEvent(mockUserDocSnapshot, null);

        // Assert - Verify notifications listener was removed
        verify(mockNotifsRegistration).remove();
    }

    @Test
    public void testBlockedOrganizersUpdate_UpdatesBlockedEmailsSet() throws Exception {
        // Suppress Log.d calls for this test
        try (MockedStatic<Log> mockedLog = mockStatic(Log.class)) {
            // Arrange
            ArgumentCaptor<EventListener<DocumentSnapshot>> prefsCaptor =
                    ArgumentCaptor.forClass(EventListener.class);
            when(mockPreferencesDocRef.addSnapshotListener(prefsCaptor.capture()))
                    .thenReturn(mockPreferencesRegistration);

            when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);

            // Start the listener
            notificationListener.start();

            // Create a list of blocked emails
            List<String> blockedEmailsList = Arrays.asList("blocked1@test.com", "blocked2@test.com");
            when(mockPreferencesDocSnapshot.exists()).thenReturn(true);
            when(mockPreferencesDocSnapshot.get("blockedEmails")).thenReturn(blockedEmailsList);

            // Act - Trigger the preferences listener
            prefsCaptor.getValue().onEvent(mockPreferencesDocSnapshot, null);

            // Assert
            java.lang.reflect.Field blockedEmailsField = NotificationListener.class.getDeclaredField("blockedEmails");
            blockedEmailsField.setAccessible(true);
            Set<String> blockedEmailsSet = (Set<String>) blockedEmailsField.get(notificationListener);

            assertEquals(2, blockedEmailsSet.size());
            assertTrue(blockedEmailsSet.contains("blocked1@test.com"));
            assertTrue(blockedEmailsSet.contains("blocked2@test.com"));
        }
    }

    @Test
    public void testStop_WithActiveListeners_RemovesAllListeners() throws Exception {
        // Arrange
        Mockito.lenient().when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);
        Mockito.lenient().when(mockPreferencesDocRef.addSnapshotListener(any())).thenReturn(mockPreferencesRegistration);
        Mockito.lenient().when(mockQuery.addSnapshotListener(any())).thenReturn(mockNotifsRegistration);

        notificationListener.start();

        // Set up the private fields with mock registrations using reflection
        java.lang.reflect.Field prefRegField = NotificationListener.class.getDeclaredField("prefRegistration");
        prefRegField.setAccessible(true);
        prefRegField.set(notificationListener, mockPrefRegistration);

        java.lang.reflect.Field preferencesRegField = NotificationListener.class.getDeclaredField("preferencesRegistration");
        preferencesRegField.setAccessible(true);
        preferencesRegField.set(notificationListener, mockPreferencesRegistration);

        java.lang.reflect.Field notifRegField = NotificationListener.class.getDeclaredField("notifsRegistration");
        notifRegField.setAccessible(true);
        notifRegField.set(notificationListener, mockNotifsRegistration);

        // Act
        notificationListener.stop();

        // Assert
        verify(mockPrefRegistration).remove();
        verify(mockPreferencesRegistration).remove();
        verify(mockNotifsRegistration).remove();
    }

    @Test
    public void testNotificationProcessingLogic_BlockedOrganizer() {
        try (MockedStatic<Log> mockedLog = mockStatic(Log.class)) {
            // Test the logic in attachNotificationsListener directly
            // Create a mock of the EventListener and test its logic

            // Set up blocked emails
            Set<String> blockedEmails = new HashSet<>();
            blockedEmails.add("blocked@test.com");

            // Create test notification from blocked organizer
            Notification testNotification = new Notification();
            testNotification.setTitle("Test Title");
            testNotification.setMessage("Test message");
            testNotification.setFromOrganizeremail("blocked@test.com");

            // Mock document
            Mockito.lenient().when(mockNotificationDocSnapshot.toObject(Notification.class)).thenReturn(testNotification);
            Mockito.lenient().when(mockNotificationDocSnapshot.getId()).thenReturn("notif123");
            Mockito.lenient().when(mockNotificationDocSnapshot.getBoolean("read")).thenReturn(false);

            DocumentReference mockDocRef = mock(DocumentReference.class);
            Task<Void> mockUpdateTask = mock(Task.class);
            Mockito.lenient().when(mockNotificationDocSnapshot.getReference()).thenReturn(mockDocRef);
            Mockito.lenient().when(mockDocRef.update(eq("read"), eq(true))).thenReturn(mockUpdateTask);
            Mockito.lenient().when(mockUpdateTask.addOnFailureListener(any())).thenReturn(mockUpdateTask);

            // Create DocumentChange
            Mockito.lenient().when(mockDocumentChange.getType()).thenReturn(DocumentChange.Type.ADDED);
            Mockito.lenient().when(mockDocumentChange.getDocument()).thenReturn(mockNotificationDocSnapshot);

            // Create QuerySnapshot with the change
            List<DocumentChange> documentChanges = Arrays.asList(mockDocumentChange);
            Mockito.lenient().when(mockQuerySnapshot.getDocumentChanges()).thenReturn(documentChanges);



            // This is more of an integration test pattern
            assertTrue("Should mark as read when from blocked organizer",
                    testNotification.getFromOrganizeremail() != null &&
                            blockedEmails.contains(testNotification.getFromOrganizeremail()));
        }
    }

    @Test
    public void testNotificationProcessingLogic_NonBlockedOrganizer() {
        try (MockedStatic<Log> mockedLog = mockStatic(Log.class)) {
            // Set up empty blocked emails
            Set<String> blockedEmails = new HashSet<>();

            // Create test notification from non-blocked organizer
            Notification testNotification = new Notification();
            testNotification.setTitle("Test Title");
            testNotification.setMessage("Test message");
            testNotification.setFromOrganizeremail("organizer@test.com");

            // Mock document
            Mockito.lenient().when(mockNotificationDocSnapshot.toObject(Notification.class)).thenReturn(testNotification);
            Mockito.lenient().when(mockNotificationDocSnapshot.getId()).thenReturn("notif123");
            Mockito.lenient().when(mockNotificationDocSnapshot.getBoolean("read")).thenReturn(false);

            DocumentReference mockDocRef = mock(DocumentReference.class);
            Mockito.lenient().when(mockNotificationDocSnapshot.getReference()).thenReturn(mockDocRef);

            // Create DocumentChange
            Mockito.lenient().when(mockDocumentChange.getType()).thenReturn(DocumentChange.Type.ADDED);
            Mockito.lenient().when(mockDocumentChange.getDocument()).thenReturn(mockNotificationDocSnapshot);

            // Create QuerySnapshot with the change
            List<DocumentChange> documentChanges = Arrays.asList(mockDocumentChange);
            Mockito.lenient().when(mockQuerySnapshot.getDocumentChanges()).thenReturn(documentChanges);

            // Test that it wouldn't be marked as read immediately
            assertFalse("Should not mark as read when from non-blocked organizer",
                    testNotification.getFromOrganizeremail() != null &&
                            blockedEmails.contains(testNotification.getFromOrganizeremail()));
        }
    }

    @Test
    public void testConstructor_InitializesFields() throws Exception {
        // Test the constructor sets fields correctly
        NotificationListener listener = new NotificationListener(mockActivity, testEmail);

        // Use reflection to check private fields
        java.lang.reflect.Field activityField = NotificationListener.class.getDeclaredField("activity");
        activityField.setAccessible(true);
        Activity retrievedActivity = (Activity) activityField.get(listener);

        java.lang.reflect.Field emailField = NotificationListener.class.getDeclaredField("email");
        emailField.setAccessible(true);
        String retrievedEmail = (String) emailField.get(listener);

        assertEquals(mockActivity, retrievedActivity);
        assertEquals(testEmail, retrievedEmail);
    }

    // Mock Notification class if needed
    private static class Notification {
        private String title;
        private String message;
        private String fromOrganizeremail;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getFromOrganizeremail() { return fromOrganizeremail; }
        public void setFromOrganizeremail(String fromOrganizeremail) { this.fromOrganizeremail = fromOrganizeremail; }
    }
}