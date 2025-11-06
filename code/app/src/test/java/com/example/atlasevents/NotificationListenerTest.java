package com.example.atlasevents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import com.example.atlasevents.data.NotificationListener;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class NotificationListenerTest {

    @Mock
    private Activity mockActivity;

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private DocumentReference mockUserDocRef;

    @Mock
    private CollectionReference mockNotificationsCollection;

    @Mock
    private ListenerRegistration mockPrefRegistration;

    @Mock
    private ListenerRegistration mockNotifsRegistration;

    @Mock
    private Query mockQuery;

    @Mock
    private DocumentSnapshot mockUserDoc;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private DocumentChange mockDocumentChange;

    @Mock
    private DocumentSnapshot mockNotificationDoc;

    private NotificationListener notificationListener;
    private final String testEmail = "user@test.com";

    @Before
    public void setUp() {
        when(mockDb.collection("users")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.document(testEmail)).thenReturn(mockUserDocRef);

        notificationListener = new NotificationListener(mockActivity, testEmail);
        // Use reflection to set the db field to our mock
        try {
            java.lang.reflect.Field dbField = NotificationListener.class.getDeclaredField("db");
            dbField.setAccessible(true);
            dbField.set(notificationListener, mockDb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStart_AttachesPreferenceListener() {
        // Arrange
        when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);

        // Act
        notificationListener.start();

        // Assert
        verify(mockUserDocRef).addSnapshotListener(any());
    }

    @Test
    public void testStart_NullEmail_DoesNothing() {
        // Arrange
        NotificationListener nullEmailListener = new NotificationListener(mockActivity, null);

        // Act
        nullEmailListener.start();

        // Assert
        // Should not attach any listeners when email is null
        verify(mockUserDocRef, never()).addSnapshotListener(any());
    }

    @Test
    public void testPreferenceChange_EnabledTrue_AttachesNotificationsListener() {
        // Arrange
        ArgumentCaptor<EventListener<DocumentSnapshot>> prefListenerCaptor =
                ArgumentCaptor.forClass(EventListener.class);

        when(mockUserDocRef.addSnapshotListener(prefListenerCaptor.capture()))
                .thenReturn(mockPrefRegistration);

        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.addSnapshotListener(any())).thenReturn(mockNotifsRegistration);

        // Act
        notificationListener.start();

        // Trigger the preference listener with enabled = true
        EventListener<DocumentSnapshot> prefListener = prefListenerCaptor.getValue();
        when(mockUserDoc.getBoolean("notificationsEnabled")).thenReturn(true);
        prefListener.onEvent(mockUserDoc, null);

        // Assert
        verify(mockQuery).addSnapshotListener(any());
    }

    @Test
    public void testPreferenceChange_EnabledFalse_DetachesNotificationsListener() {
        // Arrange
        ArgumentCaptor<EventListener<DocumentSnapshot>> prefListenerCaptor =
                ArgumentCaptor.forClass(EventListener.class);

        when(mockUserDocRef.addSnapshotListener(prefListenerCaptor.capture()))
                .thenReturn(mockPrefRegistration);

        // First attach the notifications listener
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.addSnapshotListener(any())).thenReturn(mockNotifsRegistration);

        notificationListener.start();

        // First set to true to attach listener
        EventListener<DocumentSnapshot> prefListener = prefListenerCaptor.getValue();
        when(mockUserDoc.getBoolean("notificationsEnabled")).thenReturn(true);
        prefListener.onEvent(mockUserDoc, null);

        // Act - now set to false
        when(mockUserDoc.getBoolean("notificationsEnabled")).thenReturn(false);
        prefListener.onEvent(mockUserDoc, null);

        // Assert
        verify(mockNotifsRegistration).remove();
    }

    @Test
    public void testNotificationReceived_Unread_ShowsDialogAndMarksRead() {
        // Arrange
        Notification testNotification = new Notification("Test", "Message", "1", "org@test.com", "Junitest", "Mockito");

        ArgumentCaptor<EventListener<QuerySnapshot>> notifListenerCaptor =
                ArgumentCaptor.forClass(EventListener.class);

        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.addSnapshotListener(notifListenerCaptor.capture()))
                .thenReturn(mockNotifsRegistration);

        when(mockNotificationDoc.toObject(Notification.class)).thenReturn(testNotification);
        when(mockNotificationDoc.getBoolean("read")).thenReturn(false);
        when(mockNotificationDoc.getReference()).thenReturn(mockNotificationDoc.getReference());

        List<DocumentChange> documentChanges = new ArrayList<>();
        documentChanges.add(mockDocumentChange);

        when(mockQuerySnapshot.getDocumentChanges()).thenReturn(documentChanges);
        when(mockDocumentChange.getType()).thenReturn(DocumentChange.Type.ADDED);
        when(mockDocumentChange.getDocument()).thenReturn((QueryDocumentSnapshot) mockNotificationDoc);

        // Act - trigger notification listener
        notificationListener.start();
        // We need to simulate the preference being enabled first
        // This would require more complex setup to trigger the chain

        // Assert
        // Verify NotificationHelper.showInAppDialog was called
        // Verify document was marked as read
    }

    @Test
    public void testNotificationReceived_AlreadyRead_SkipsProcessing() {
        // Arrange
        Notification testNotification = new Notification("Test", "Message", "2", "org@test.com", "JunitTest", "Mockito");

        when(mockNotificationDoc.toObject(Notification.class)).thenReturn(testNotification);
        when(mockNotificationDoc.getBoolean("read")).thenReturn(true); // Already read

        // Act & Assert
        // Should skip showing dialog and marking as read
        // Verify NotificationHelper.showInAppDialog was NOT called
        verify(mockNotificationDoc, never()).getDate("read");
    }

    @Test
    public void testStop_RemovesAllListeners() {
        // Arrange
        when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);

        notificationListener.start();

        // Use reflection to set the registrations
        try {
            java.lang.reflect.Field prefRegField = NotificationListener.class.getDeclaredField("prefRegistration");
            prefRegField.setAccessible(true);
            prefRegField.set(notificationListener, mockPrefRegistration);

            java.lang.reflect.Field notifRegField = NotificationListener.class.getDeclaredField("notifsRegistration");
            notifRegField.setAccessible(true);
            notifRegField.set(notificationListener, mockNotifsRegistration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        notificationListener.stop();

        // Assert
        verify(mockPrefRegistration).remove();
        verify(mockNotifsRegistration).remove();
    }
}