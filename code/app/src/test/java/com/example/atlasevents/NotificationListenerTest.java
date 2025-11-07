package com.example.atlasevents;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.app.Activity;

import com.example.atlasevents.data.NotificationListener;
import com.google.firebase.firestore.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

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
    private ListenerRegistration mockPrefRegistration;
    @Mock
    private DocumentSnapshot mockUserDocSnapshot;
    @Mock
    private ListenerRegistration mockNotifsRegistration;

    // --- Static Mock Controller ---
    // This will control the static methods of FirebaseFirestore
    private MockedStatic<FirebaseFirestore> mockedFirestore;

    private NotificationListener notificationListener;
    private final String testEmail = "user@test.com";



    @Before
    public void setUp() {
        /**
         * Sets up test fixtures before each test method.
         * Creates NotificationListener with mocked dependencies.*/
        mockedFirestore = mockStatic(FirebaseFirestore.class);

        // Tell the static mock that whenever getInstance() is called, return our mockDb
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);


        notificationListener = new NotificationListener(mockActivity, testEmail);

        // --- STEP 3: Set up the mock chain for database interactions ---
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(testEmail)).thenReturn(mockUserDocRef);
    }

    @After
    public void tearDown() {
        // This releases the mock and prevents it from interfering with other tests.
        mockedFirestore.close();
    }
    /**
     //     * Tests that start method attaches preference listener when email is provided.
     //     * Verifies that the user document snapshot listener is registered.
     //     */
    @Test
    public void testStart_WithValidEmail_AttachesPreferenceListener() {
        // Arrange
        when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);

        // Act
        notificationListener.start();

        // Assert
        verify(mockUserDocRef).addSnapshotListener(any(EventListener.class));
    }
    /**
         * Tests that start method does nothing when email is null.
          * Verifies that no Firestore operations are performed with null email.
          */
    @Test
    public void testStart_WithNullEmail_DoesNothing() {
        // Arrange
        // We can create the listener with a null email now without it crashing
        NotificationListener nullEmailListener = new NotificationListener(mockActivity, null);

        // Act
        nullEmailListener.start();

        // Assert
        // Verify that no database interactions occurred
        verify(mockDb, never()).collection(anyString());
    }

    @Test
    public void testPreferenceChange_ToEnabled_AttachesNotificationsListener() {
        // Arrange
        ArgumentCaptor<EventListener<DocumentSnapshot>> captor = ArgumentCaptor.forClass(EventListener.class);
        when(mockUserDocRef.addSnapshotListener(captor.capture())).thenReturn(mockPrefRegistration);

        // Set up the mock chain for the second listener attachment
        CollectionReference mockNotificationsCollection = mock(CollectionReference.class);
        Query mockQuery = mock(Query.class);
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);
        when(mockQuery.addSnapshotListener(any())).thenReturn(mockNotifsRegistration);

        // Call start() to attach the first listener
        notificationListener.start();

        // Manually trigger the captured listener
        when(mockUserDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);
        captor.getValue().onEvent(mockUserDocSnapshot, null);

        // Assert
        // Verify the second listener was attached
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollection).orderBy(eq("createdAt"), any(Query.Direction.class));
    }

    @Test
    public void testStop_WithActiveListeners_RemovesAllListeners() throws NoSuchFieldException, IllegalAccessException {
        // Arrange
        // Simulate that start() was called and listeners were attached
        when(mockUserDocRef.addSnapshotListener(any())).thenReturn(mockPrefRegistration);
        notificationListener.start();

        // To test that 'stop' removes the listener, we still need to manually place
        // a mock registration object into the private field using reflection,
        // because the real addSnapshotListener call inside start() returns null in a test.
        java.lang.reflect.Field prefRegField = NotificationListener.class.getDeclaredField("prefRegistration");
        prefRegField.setAccessible(true);
        prefRegField.set(notificationListener, mockPrefRegistration);

        // Act
        notificationListener.stop();

        // Assert
        verify(mockPrefRegistration).remove();
    }
}
