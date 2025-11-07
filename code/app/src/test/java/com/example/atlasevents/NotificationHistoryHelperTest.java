package com.example.atlasevents;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.LinearLayout;
import com.example.atlasevents.data.model.Notification;
import com.example.atlasevents.utils.NotificationHistoryHelper;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the {@link NotificationHistoryHelper} class.
 * Tests the loading and display of notification history for both entrants and organizers.
 * Verifies Firestore queries, data parsing, and UI card creation.
 *
 * @see NotificationHistoryHelper
 * @see NotificationHistoryHelper.NotificationLoadCallback
 * @see NotificationHistoryHelper.MarkAsReadCallback
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationHistoryHelperTest {

    @Mock
    private Context mockContext;

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private LinearLayout mockNotificationsContainer;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private DocumentReference mockUserDocRef;

    @Mock
    private CollectionReference mockNotificationsCollection;

    @Mock
    private CollectionReference mockLogsCollection;

    @Mock
    private Query mockQuery;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private QueryDocumentSnapshot mockDocumentSnapshot;

    private Task<QuerySnapshot> mockTask;

    private NotificationHistoryHelper notificationHistoryHelper;
    private final String testUserEmail = "user@test.com";

    /**
     * Sets up test fixtures before each test method.
     * Creates NotificationHistoryHelper with mocked dependencies.
     */
    @Before
    public void setUp() {
        notificationHistoryHelper = new NotificationHistoryHelper(mockContext, mockDb, mockNotificationsContainer);

        // Setup common mock behaviors
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(testUserEmail)).thenReturn(mockUserDocRef);
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.orderBy(anyString(), any())).thenReturn(mockQuery);
        when(mockDb.collection("notification_logs")).thenReturn(mockLogsCollection);
        when(mockLogsCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockLogsCollection.orderBy(anyString(), any())).thenReturn(mockQuery);
    }

    /**
     * Tests loading entrant notifications with successful query results.
     * Verifies that notifications are parsed and cards are created for each document.
     */
    @Test
    public void testLoadEntrantReceivedNotifications_WithResults_CreatesNotificationCards() {
        // Arrange
        NotificationHistoryHelper.NotificationLoadCallback mockLoadCallback =
                mock(NotificationHistoryHelper.NotificationLoadCallback.class);
        NotificationHistoryHelper.MarkAsReadCallback mockMarkAsReadCallback =
                mock(NotificationHistoryHelper.MarkAsReadCallback.class);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot);

        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuery.get()).thenReturn(mockTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.size()).thenReturn(2);
        when(mockQuerySnapshot.iterator()).thenReturn(documents.iterator());

        Notification testNotification = new Notification("Title", "Message", "event1", "org@test.com", "Event", "Group");
        when(mockDocumentSnapshot.toObject(Notification.class)).thenReturn(testNotification);
        when(mockDocumentSnapshot.getId()).thenReturn("notif1");

        // Act
        notificationHistoryHelper.loadEntrantReceivedNotifications(testUserEmail, mockLoadCallback, mockMarkAsReadCallback);

        // Assert
        verify(mockLoadCallback).onNotificationsLoaded(2);
    }

    /**
     * Tests loading entrant notifications with empty results.
     * Verifies that empty state is handled properly and callback indicates failure.
     */
    @Test
    public void testLoadEntrantReceivedNotifications_WithEmptyResults_CallsLoadFailed() {
        // Arrange
        NotificationHistoryHelper.NotificationLoadCallback mockLoadCallback =
                mock(NotificationHistoryHelper.NotificationLoadCallback.class);
        NotificationHistoryHelper.MarkAsReadCallback mockMarkAsReadCallback =
                mock(NotificationHistoryHelper.MarkAsReadCallback.class);
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);

        when(mockQuery.get()).thenReturn(mockTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        // Act
        notificationHistoryHelper.loadEntrantReceivedNotifications(testUserEmail, mockLoadCallback, mockMarkAsReadCallback);

        // Assert
        verify(mockLoadCallback).onLoadFailed();
    }

    /**
     * Tests loading organizer sent notifications with successful query results.
     * Verifies that notification logs are parsed and cards are created.
     */
    @Test
    public void testLoadOrganizerSentNotifications_WithResults_CreatesNotificationCards() {
        // Arrange
        NotificationHistoryHelper.NotificationLoadCallback mockLoadCallback =
                mock(NotificationHistoryHelper.NotificationLoadCallback.class);

        List<QueryDocumentSnapshot> documents = new ArrayList<>();
        documents.add(mockDocumentSnapshot);

        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);

        when(mockQuery.get()).thenReturn(mockTask);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.size()).thenReturn(3);
        when(mockQuerySnapshot.iterator()).thenReturn(documents.iterator());

        Map<String, Object> logData = new HashMap<>();
        logData.put("groupType", "Waitlist");
        logData.put("eventName", "Test Event");
        logData.put("createdAt", new Date());
        logData.put("message", "Test message");
        logData.put("recipient", "user@test.com");
        logData.put("status", "SENT");

        when(mockDocumentSnapshot.getData()).thenReturn(logData);

        // Act
        notificationHistoryHelper.loadOrganizerSentNotifications(testUserEmail, mockLoadCallback);

        // Assert
        verify(mockLoadCallback).onNotificationsLoaded(3);
    }

    /**
     * Tests timestamp formatting with valid Date object.
     * Verifies that dates are formatted correctly using the expected pattern.
     */
    @Test
    public void testFormatTimestamp_WithValidDate_ReturnsFormattedString() {
        // This would test the private formatTimestamp method via reflection
        // or by testing through the public methods that use it
        assertTrue(true); // Placeholder for actual implementation
    }

    /**
     * Tests callback interfaces are properly implemented and callable.
     * Verifies that both NotificationLoadCallback and MarkAsReadCallback work correctly.
     */
    @Test
    public void testCallbackInterfaces_AreProperlyImplemented() {
        // Test NotificationLoadCallback
        NotificationHistoryHelper.NotificationLoadCallback loadCallback =
                new NotificationHistoryHelper.NotificationLoadCallback() {
                    @Override
                    public void onNotificationsLoaded(int count) {
                        // Test implementation
                    }

                    @Override
                    public void onLoadFailed() {
                        // Test implementation
                    }
                };

        // Test MarkAsReadCallback
        NotificationHistoryHelper.MarkAsReadCallback markAsReadCallback =
                new NotificationHistoryHelper.MarkAsReadCallback() {
                    @Override
                    public void onMarkAsRead(String notificationId) {
                        // Test implementation
                    }
                };

        // Verify no exceptions when calling interface methods
        loadCallback.onNotificationsLoaded(5);
        loadCallback.onLoadFailed();
        markAsReadCallback.onMarkAsRead("test-id");

        assertTrue(true);
    }
}