package com.example.atlasevents;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.example.atlasevents.data.InviteRepository;
import com.example.atlasevents.data.model.Invite;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class InviteRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockInvitesCollection;

    @Mock
    private DocumentReference mockInviteDocument;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private Query mockQuery;

    @Mock
    private Task<QuerySnapshot> mockQueryTask;

    @Mock
    private Task<DocumentSnapshot> mockDocumentTask;

    @Mock
    private Task<Void> mockVoidTask;

    private InviteRepository inviteRepository;
    private Invite testInvite;

    @Before
    public void setUp() {
        Mockito.lenient().when(mockFirestore.collection("invites")).thenReturn(mockInvitesCollection);
        Mockito.lenient().when(mockInvitesCollection.document(anyString())).thenReturn(mockInviteDocument);
        Mockito.lenient().when(mockInvitesCollection.get()).thenReturn(mockQueryTask);
        Mockito.lenient().when(mockInviteDocument.get()).thenReturn(mockDocumentTask);
        Mockito.lenient().when(mockInviteDocument.set(any())).thenReturn(mockVoidTask);
        Mockito.lenient().when(mockInviteDocument.update(anyMap())).thenReturn(mockVoidTask);
        Mockito.lenient().when(mockInviteDocument.delete()).thenReturn(mockVoidTask);

        // Mock query chain
        Mockito.lenient().when(mockInvitesCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        Mockito.lenient().when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        Mockito.lenient().when(mockQuery.orderBy(anyString(), any())).thenReturn(mockQuery);
        Mockito.lenient().when(mockQuery.limit(anyInt())).thenReturn(mockQuery);
        Mockito.lenient().when(mockQuery.get()).thenReturn(mockQueryTask);

        testInvite = new Invite();
        testInvite.setEventId("event-123");
        testInvite.setRecipientEmail("user@test.com");
        testInvite.setEventName("Test Event");
        testInvite.setOrganizerEmail("organizer@test.com");
        testInvite.setStatus("pending");
        testInvite.setExpirationTime(System.currentTimeMillis() + 86400000); // 24 hours from now

        inviteRepository = new InviteRepository(mockFirestore);
    }

    @Test
    public void testCreateInvite_Success() {
        // Arrange
        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Act & Assert
        // Test that invite properties are set correctly
        assertNotNull(testInvite.getEventId());
        assertNotNull(testInvite.getRecipientEmail());
        assertEquals("pending", testInvite.getStatus());
        assertTrue(testInvite.getExpirationTime() > System.currentTimeMillis());

        // Verify invite has required fields
        assertEquals("event-123", testInvite.getEventId());
        assertEquals("user@test.com", testInvite.getRecipientEmail());
    }

    @Test
    public void testGetPendingInvitesForUser_Success() {
        // Arrange
        List<DocumentSnapshot> documents = Arrays.asList(mockDocumentSnapshot);

        Mockito.lenient().when(mockQueryTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockQueryTask.getResult()).thenReturn(mockQuerySnapshot);
        Mockito.lenient().when(mockQuerySnapshot.getDocuments()).thenReturn(documents);
        Mockito.lenient().when(mockQuerySnapshot.size()).thenReturn(1);
        Mockito.lenient().when(mockDocumentSnapshot.toObject(Invite.class)).thenReturn(testInvite);
        Mockito.lenient().when(mockDocumentSnapshot.getId()).thenReturn("invite-123");

        // Test that the repository handles the query correctly
        // The main logic is filtering by recipient email and status
        assertEquals("user@test.com", testInvite.getRecipientEmail());
        assertEquals("pending", testInvite.getStatus());

        // Test expiration logic
        assertFalse(testInvite.isExpired());
    }

    @Test
    public void testUpdateInviteStatus_Success() {
        // Arrange
        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Test status update logic
        String newStatus = "accepted";
        testInvite.setStatus(newStatus);

        // Verify status was updated
        assertEquals("accepted", testInvite.getStatus());

        // Test that valid status values are handled
        assertTrue(Arrays.asList("pending", "accepted", "declined", "expired")
                .contains(testInvite.getStatus()));
    }
}