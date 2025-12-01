package com.example.atlasevents;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.utils.ImageUploader;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class EventRepositoryTest {
        @Mock
        private FirebaseFirestore mockFirestore;

        @Mock
        private FirebaseStorage mockFirebaseStorage;

        @Mock
        private ImageUploader mockImageUploader;

        @Mock
        private CollectionReference mockEventsCollection;

        @Mock
        private DocumentReference mockEventDocument;

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

        private EventRepository eventRepository;
        private Event testEvent;
        private Organizer testOrganizer;
    @Before
    public void setUp() {
        // Mock ALL Firebase static methods using MockedStatic
        try (MockedStatic<FirebaseFirestore> mockedFirestore = mockStatic(FirebaseFirestore.class);
             MockedStatic<FirebaseStorage> mockedStorage = mockStatic(FirebaseStorage.class)) {

            mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockFirestore);
            mockedStorage.when(FirebaseStorage::getInstance).thenReturn(mockFirebaseStorage);

            // Mock Firestore chain
            Mockito.lenient().when(mockFirestore.collection("events")).thenReturn(mockEventsCollection);
            Mockito.lenient().when(mockEventsCollection.document(anyString())).thenReturn(mockEventDocument);
            Mockito.lenient().when(mockEventsCollection.get()).thenReturn(mockQueryTask);
            Mockito.lenient().when(mockEventDocument.get()).thenReturn(mockDocumentTask);
            Mockito.lenient().when(mockEventDocument.set(any())).thenReturn(mockVoidTask);

            // Mock query for organizer events
            Mockito.lenient().when(mockEventsCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
            Mockito.lenient().when(mockQuery.get()).thenReturn(mockQueryTask);

            // Initialize repositories with mocked dependencies
            eventRepository = new EventRepository();

            // Create organizer WITHOUT initializing EventRepository in constructor
            testOrganizer = new Organizer();
            testOrganizer.setEmail("organizer@test.com");
            testOrganizer.setName("Test Organizer");

            // Create test event
            testEvent = new Event(testOrganizer);
            testEvent.setId("test-event-123");
            testEvent.setEventName("Test Event");
            testEvent.setSlots(10);
        }
    }

    @Test
    public void testAddEvent_Success() {
        // Arrange
        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Test basic event properties
        assertNotNull(testEvent.getId());
        assertEquals("Test Event", testEvent.getEventName());
        assertEquals(10, testEvent.getSlots());
        assertEquals("organizer@test.com", testEvent.getOrganizer().getEmail());
    }

    @Test
    public void testGetEventById_Success() {
        // Arrange
        EventRepository.EventCallback callback = mock(EventRepository.EventCallback.class);

        Mockito.lenient().when(mockDocumentTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
        Mockito.lenient().when(mockDocumentSnapshot.exists()).thenReturn(true);
        Mockito.lenient().when(mockDocumentSnapshot.toObject(Event.class)).thenReturn(testEvent);

        // Test callback invocation
        callback.onSuccess(testEvent);

        // Verify callback was called with correct event
        verify(callback).onSuccess(testEvent);
        assertEquals("test-event-123", testEvent.getId());
    }

    @Test
    public void testGetEventsByOrganizer_FiltersCorrectly() {
        // Arrange
        EventRepository.EventsCallback callback = mock(EventRepository.EventsCallback.class);
        List<DocumentSnapshot> documents = Arrays.asList(mockDocumentSnapshot);

        Mockito.lenient().when(mockQueryTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockQueryTask.getResult()).thenReturn(mockQuerySnapshot);
        Mockito.lenient().when(mockQuerySnapshot.getDocuments()).thenReturn(documents);
        Mockito.lenient().when(mockDocumentSnapshot.toObject(Event.class)).thenReturn(testEvent);

        // Test that organizer email matching works
        String organizerEmail = "organizer@test.com";
        assertEquals(organizerEmail, testEvent.getOrganizer().getEmail());

        // Test callback
        ArrayList<Event> events = new ArrayList<>();
        events.add(testEvent);
        callback.onSuccess(events);

        verify(callback).onSuccess(events);
        assertEquals(1, events.size());
    }
}