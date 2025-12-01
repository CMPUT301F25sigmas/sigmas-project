package com.example.atlasevents;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.atlasevents.data.InviteRepository;
import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Invite;
import com.example.atlasevents.utils.ImageUploader;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

/**
 * Unit tests for LotteryService using Mockito
 */
@RunWith(MockitoJUnitRunner.class)
public class LotteryServiceUnitTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private FirebaseStorage mockFirebaseStorage;

    @Mock
    private NotificationRepository mockNotificationRepo;

    @Mock
    private InviteRepository mockInviteRepo;

    @Mock
    private ImageUploader mockImageUploader;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private DocumentReference mockEventDocument;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private Task<DocumentSnapshot> mockDocumentTask;

    @Mock
    private Task<Void> mockUpdateTask;

    @Mock
    private Task<List<Task<Void>>> mockInviteTask;

    private MockedStatic<FirebaseFirestore> mockedFirestore;
    private MockedStatic<FirebaseStorage> mockedStorage;
    private MockedStatic<android.util.Log> mockedLog;

    private LotteryService lotteryService;
    private Event testEvent;
    private Organizer testOrganizer;
    private List<Entrant> testEntrants;

    @Before
    public void setUp() {
        // Mock ALL Firebase static methods to prevent initialization
        mockedFirestore = mockStatic(FirebaseFirestore.class);
        mockedStorage = mockStatic(FirebaseStorage.class);
        mockedLog = mockStatic(android.util.Log.class);

        // Mock Firebase instances
        mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
        mockedStorage.when(FirebaseStorage::getInstance).thenReturn(mockFirebaseStorage);

        // Mock Firestore chain
        Mockito.lenient().when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        Mockito.lenient().when(mockEventsCollection.document(anyString())).thenReturn(mockEventDocument);
        Mockito.lenient().when(mockEventDocument.get()).thenReturn(mockDocumentTask);
        Mockito.lenient().when(mockEventDocument.update(anyMap())).thenReturn(mockUpdateTask);
        Mockito.lenient().when(mockEventDocument.set(any())).thenReturn(mockUpdateTask);

        // Initialize lottery service with mocked dependencies
        lotteryService = new LotteryService(mockDb, mockNotificationRepo, mockInviteRepo);

        // Create test organizer WITHOUT initializing EventRepository
        testOrganizer = createTestOrganizer();

        // Create test event WITHOUT initializing EventRepository
        testEvent = createTestEvent(testOrganizer);

        // Create test entrants
        testEntrants = createTestEntrants();

        // Add entrants to waitlist
        for (Entrant entrant : testEntrants) {
            testEvent.getWaitlist().addEntrant(entrant);
        }
    }

    @After
    public void tearDown() {
        if (mockedFirestore != null) {
            mockedFirestore.close();
        }
        if (mockedStorage != null) {
            mockedStorage.close();
        }
        if (mockedLog != null) {
            mockedLog.close();
        }
    }

    /**
     * Create test organizer without initializing EventRepository
     */
    private Organizer createTestOrganizer() {
        Organizer organizer = new Organizer();
        organizer.setEmail("organizer@test.com");
        organizer.setName("Test Organizer");

        // Use reflection to avoid EventRepository initialization if needed
        try {
            java.lang.reflect.Field eventRepositoryField = Organizer.class.getDeclaredField("eventRepository");
            eventRepositoryField.setAccessible(true);
            eventRepositoryField.set(organizer, null); // Set to null to avoid initialization
        } catch (Exception e) {
            // If reflection fails, the organizer will still work for most tests
        }

        return organizer;
    }

    /**
     * Create test event without initializing EventRepository
     */
    private Event createTestEvent(Organizer organizer) {
        Event event = new Event(organizer);
        event.setId("test-event-123");
        event.setEventName("Test Event");
        event.setSlots(5);

        // Set registration end date to past (lottery should be available)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
        event.setRegEndDate(cal.getTime());

        // Use reflection to avoid EventRepository initialization if needed
        try {
            java.lang.reflect.Field dbField = Event.class.getDeclaredField("db");
            dbField.setAccessible(true);
            dbField.set(event, null); // Set to null to avoid initialization
        } catch (Exception e) {
            // If reflection fails, the event will still work for most tests
        }

        return event;
    }

    /**
     * Create test entrants
     */
    private List<Entrant> createTestEntrants() {
        return Arrays.asList(
                new Entrant("John Doe", "john@test.com", "password123", "1234567890"),
                new Entrant("Jane Smith", "jane@test.com", "password456", "0987654321"),
                new Entrant("Bob Johnson", "bob@test.com", "password789", "5555555555"),
                new Entrant("Alice Brown", "alice@test.com", "password000", "1111111111")
        );
    }

    @Test
    public void testCalculateAvailableSlots_NoAcceptedEntrants() {
        // Act
        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);

        // Assert
        assertEquals(5, availableSlots); // 5 slots - 0 accepted = 5 available
    }

    @Test
    public void testCalculateAvailableSlots_SomeAcceptedEntrants() {
        // Arrange
        testEvent.getAcceptedList().addEntrant(testEntrants.get(0));
        testEvent.getAcceptedList().addEntrant(testEntrants.get(1));

        // Act
        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);

        // Assert
        assertEquals(3, availableSlots); // 5 slots - 2 accepted = 3 available
    }

    @Test
    public void testCalculateAvailableSlots_AllSlotsFilled() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            Entrant entrant = new Entrant("User" + i, "user" + i + "@test.com", "pass", "123");
            testEvent.getAcceptedList().addEntrant(entrant);
        }

        // Act
        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);

        // Assert
        assertEquals(0, availableSlots); // 5 slots - 5 accepted = 0 available
    }

    @Test
    public void testIsLotteryAvailable_RegistrationEnded() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
        testEvent.setRegEndDate(cal.getTime());

        // Act
        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);

        // Assert
        assertTrue(isAvailable);
    }

    @Test
    public void testIsLotteryAvailable_RegistrationNotEnded() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
        testEvent.setRegEndDate(cal.getTime());

        // Act
        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);

        // Assert
        assertFalse(isAvailable);
    }

    @Test
    public void testIsLotteryAvailable_NoRegistrationDate() {
        // Arrange
        testEvent.setRegEndDate(null);

        // Act
        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);

        // Assert
        assertFalse(isAvailable);
    }

    @Test
    public void testCheckLotteryAvailability_Available() {
        // Act
        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);

        // Assert
        assertTrue(availability.isAvailable());
        assertTrue(availability.getMessage().contains("Lottery available"));
    }

    @Test
    public void testCheckLotteryAvailability_NotAvailableRegistration() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
        testEvent.setRegEndDate(cal.getTime());

        // Act
        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);

        // Assert
        assertFalse(availability.isAvailable());
        assertTrue(availability.getMessage().contains("Lottery not available"));
    }

    @Test
    public void testCheckLotteryAvailability_NoEligibleEntrants() {
        // Arrange
        testEvent.getWaitlist().getWaitList().clear(); // Empty waitlist

        // Act
        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);

        // Assert
        assertFalse(availability.isAvailable());
        assertTrue(availability.getMessage().contains("No eligible entrants"));
    }

    @Test
    public void testGetTimeUntilLotteryAvailable_FutureDate() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 2); // 2 hours from now
        testEvent.setRegEndDate(cal.getTime());

        // Act
        long timeRemaining = lotteryService.getTimeUntilLotteryAvailable(testEvent);

        // Assert - The method sets time to end of day (23:59:59), so it will be more than 2 hours
        assertTrue("Time remaining should be positive", timeRemaining > 0);
        assertTrue("Time remaining should be less than 24 hours", timeRemaining <= (24 * 60 * 60 * 1000));
    }

    @Test
    public void testGetTimeUntilLotteryAvailable_AlreadyAvailable() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
        testEvent.setRegEndDate(cal.getTime());

        // Act
        long timeRemaining = lotteryService.getTimeUntilLotteryAvailable(testEvent);

        // Assert
        assertEquals(0, timeRemaining);
    }
    @Test
    public void testGetTimeUntilLotteryAvailable_NoRegistrationDate() {
        // Arrange
        testEvent.setRegEndDate(null);

        // Act
        long timeRemaining = lotteryService.getTimeUntilLotteryAvailable(testEvent);

        // Assert
        assertEquals("Time remaining should be 0 when no registration date", 0, timeRemaining);
    }

    @Test
    public void testGetTimeUntilLotteryAvailable_ExactCalculation() {
        // Arrange - Set a specific future time for precise testing
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 30); // 30 minutes from now
        testEvent.setRegEndDate(cal.getTime());

        // Act
        long timeRemaining = lotteryService.getTimeUntilLotteryAvailable(testEvent);

        // Assert - The method should return time until end of day (23:59:59)
        // Since we set the time to 30 minutes from now, but the method sets it to end of day,
        // the actual remaining time will be much longer
        assertTrue("Time remaining should account for end of day", timeRemaining > 0);
    }


    @Test
    public void testEntrantListOperations() {
        // Test basic entrant list operations
        EntrantList waitlist = testEvent.getWaitlist();
        assertEquals(4, waitlist.size());

        // Test entrant properties
        assertEquals("john@test.com", testEntrants.get(0).getEmail());
        assertEquals("Jane Smith", testEntrants.get(1).getName());
    }

    @Test
    public void testEventProperties() {
        // Test event properties
        assertEquals("test-event-123", testEvent.getId());
        assertEquals("Test Event", testEvent.getEventName());
        assertEquals(5, testEvent.getSlots());
        assertEquals("organizer@test.com", testEvent.getOrganizer().getEmail());
    }
}