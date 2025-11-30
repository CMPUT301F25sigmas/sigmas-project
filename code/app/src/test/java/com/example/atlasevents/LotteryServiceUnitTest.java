package com.example.atlasevents;

//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.junit.Assert.*;
//
//import com.google.android.gms.tasks.Task;
//import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.Timestamp;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QuerySnapshot;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.util.*;
//
//@RunWith(MockitoJUnitRunner.class)
//public class LotteryServiceTest {
//
//    @Mock
//    private FirebaseFirestore mockFirestore;
//
//    @Mock
//    private NotificationRepository mockNotificationRepo;
//
//    @Mock
//    private DocumentSnapshot mockDocumentSnapshot;
//
//    @Mock
//    private Task<DocumentSnapshot> mockDocumentTask;
//
//    @Mock
//    private Task<Void> mockUpdateTask;
//
//    @Mock
//    private Task<QuerySnapshot> mockQueryTask;
//
//    @Mock
//    private QuerySnapshot mockQuerySnapshot;
//
//    private LotteryService lotteryService;
//    private Event testEvent;
//    private Organizer testOrganizer;
//    private List<Entrant> testEntrants;
//
//    @Before
//    public void setUp() {
//        lotteryService = new LotteryService(mockFirestore, mockNotificationRepo);
//
//        // Create test organizer
//        testOrganizer = new Organizer();
//        testOrganizer.setEmail("organizer@test.com");
//        testOrganizer.setName("Test Organizer");
//
//        // Create test event
//        testEvent = new Event(testOrganizer);
//        testEvent.setId("test-event-123");
//        testEvent.setEventName("Test Event");
//        testEvent.setSlots(2);
//
//        // Set registration end date to past (lottery should be available)
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
//        testEvent.setRegEndDate(cal.getTime());
//
//        // Create test entrants
//        testEntrants = Arrays.asList(
//                new Entrant("John Doe", "john@test.com", "password123", "1234567890"),
//                new Entrant("Jane Smith", "jane@test.com", "password456", "0987654321"),
//                new Entrant("Bob Johnson", "bob@test.com", "password789", "5555555555")
//        );
//
//        // Add entrants to waitlist
//        for (Entrant entrant : testEntrants) {
//            testEvent.getWaitlist().addEntrant(entrant);
//        }
//    }
//
//    @Test
//    public void testDrawLottery_Success() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
//        when(mockDocumentSnapshot.exists()).thenReturn(true);
//
//        // Mock document data
//        Map<String, Object> eventData = createMockEventData();
//        when(mockDocumentSnapshot.getData()).thenReturn(eventData);
//        when(mockDocumentSnapshot.getId()).thenReturn("test-event-123");
//        when(mockDocumentSnapshot.getString("eventName")).thenReturn("Test Event");
//        when(mockDocumentSnapshot.getLong("entrantLimit")).thenReturn(-1L);
//        when(mockDocumentSnapshot.contains("inviteList")).thenReturn(true);
//        when(mockDocumentSnapshot.get("inviteList")).thenReturn(new HashMap<>());
//
//        // Mock update operation
//        when(mockFirestore.collection("events").document(anyString()).update(anyMap()))
//                .thenReturn(mockUpdateTask);
//        when(mockUpdateTask.isSuccessful()).thenReturn(true);
//
//        // Mock notification operation
//        when(mockNotificationRepo.sendToUsers(anyList(), any(Notification.class)))
//                .thenReturn(Tasks.forResult(null));
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.drawLottery("test-event-123", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryCompleted(2,
//                "Lottery completed. 2 entrants notified.");
//    }
//
//    @Test
//    public void testDrawLottery_EventNotFound() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(null);
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.drawLottery("non-existent-event", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryFailed(any(Exception.class));
//    }
//
//    @Test
//    public void testDrawLottery_RegistrationNotEnded() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
//        when(mockDocumentSnapshot.exists()).thenReturn(true);
//
//        // Set registration end date to future
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
//        Map<String, Object> eventData = createMockEventData();
//        eventData.put("regEndDate", new Timestamp(cal.getTime()));
//        when(mockDocumentSnapshot.getData()).thenReturn(eventData);
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.drawLottery("test-event-123", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryFailed(any(Exception.class));
//    }
//
//    @Test
//    public void testDrawLottery_NoAvailableSlots() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
//        when(mockDocumentSnapshot.exists()).thenReturn(true);
//
//        // Event with no available slots (all slots filled)
//        Map<String, Object> eventData = createMockEventData();
//        Map<String, Object> acceptedListData = new HashMap<>();
//        acceptedListData.put("allEntrants", createEntrantListData(Arrays.asList(testEntrants.get(0), testEntrants.get(1))));
//        acceptedListData.put("waitList", createEntrantListData(Arrays.asList(testEntrants.get(0), testEntrants.get(1))));
//        eventData.put("acceptedList", acceptedListData);
//
//        when(mockDocumentSnapshot.getData()).thenReturn(eventData);
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.drawLottery("test-event-123", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryCompleted(0, "No available slots for lottery");
//    }
//
//    @Test
//    public void testDrawLottery_NoEligibleEntrants() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
//        when(mockDocumentSnapshot.exists()).thenReturn(true);
//
//        // Empty waitlist
//        Map<String, Object> eventData = createMockEventData();
//        Map<String, Object> waitlistData = new HashMap<>();
//        waitlistData.put("allEntrants", new ArrayList<>());
//        waitlistData.put("waitList", new ArrayList<>());
//        eventData.put("waitlist", waitlistData);
//
//        when(mockDocumentSnapshot.getData()).thenReturn(eventData);
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.drawLottery("test-event-123", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryCompleted(0, "No eligible entrants in waitlist");
//    }
//
//    @Test
//    public void testCalculateAvailableSlots() {
//        // Arrange
//        testEvent.getAcceptedList().addEntrant(testEntrants.get(0)); // One accepted entrant
//
//        // Act
//        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);
//
//        // Assert
//        assertEquals(1, availableSlots); // 2 slots - 1 accepted = 1 available
//    }
//
//    @Test
//    public void testCalculateAvailableSlots_NoAcceptedEntrants() {
//        // Act
//        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);
//
//        // Assert
//        assertEquals(2, availableSlots); // 2 slots - 0 accepted = 2 available
//    }
//
//    @Test
//    public void testCalculateAvailableSlots_AllSlotsFilled() {
//        // Arrange
//        testEvent.getAcceptedList().addEntrant(testEntrants.get(0));
//        testEvent.getAcceptedList().addEntrant(testEntrants.get(1)); // Both slots filled
//
//        // Act
//        int availableSlots = lotteryService.calculateAvailableSlots(testEvent);
//
//        // Assert
//        assertEquals(0, availableSlots); // 2 slots - 2 accepted = 0 available
//    }
//
//    @Test
//    public void testIsLotteryAvailable_RegistrationEnded() {
//        // Arrange
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
//        testEvent.setRegEndDate(cal.getTime());
//
//        // Act
//        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);
//
//        // Assert
//        assertTrue(isAvailable);
//    }
//
//    @Test
//    public void testIsLotteryAvailable_RegistrationNotEnded() {
//        // Arrange
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
//        testEvent.setRegEndDate(cal.getTime());
//
//        // Act
//        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);
//
//        // Assert
//        assertFalse(isAvailable);
//    }
//
//    @Test
//    public void testIsLotteryAvailable_NoRegistrationDate() {
//        // Arrange
//        testEvent.setRegEndDate(null);
//
//        // Act
//        boolean isAvailable = lotteryService.isLotteryAvailable(testEvent);
//
//        // Assert
//        assertFalse(isAvailable);
//    }
//
//    @Test
//    public void testFilterEligibleEntrants() {
//        // Arrange - Add some entrants to other lists
//        testEvent.getInviteList().addEntrant(testEntrants.get(0)); // John is invited
//        testEvent.getAcceptedList().addEntrant(testEntrants.get(1)); // Jane is accepted
//
//        // Act
//        List<Entrant> eligible = lotteryService.filterEligibleEntrants(testEvent.getWaitlist(), testEvent);
//
//        // Assert
//        assertEquals(1, eligible.size()); // Only Bob should be eligible
//        assertEquals("bob@test.com", eligible.get(0).getEmail());
//    }
//
//    @Test
//    public void testFilterEligibleEntrants_AllEligible() {
//        // Act
//        List<Entrant> eligible = lotteryService.filterEligibleEntrants(testEvent.getWaitlist(), testEvent);
//
//        // Assert
//        assertEquals(3, eligible.size()); // All entrants should be eligible
//    }
//
//    @Test
//    public void testSelectRandomEntrants() {
//        // Arrange
//        List<Entrant> entrants = Arrays.asList(testEntrants.get(0), testEntrants.get(1));
//
//        // Act
//        List<Entrant> selected = lotteryService.selectRandomEntrants(entrants, 1);
//
//        // Assert
//        assertEquals(1, selected.size());
//        assertTrue(entrants.contains(selected.get(0)));
//    }
//
//    @Test
//    public void testSelectRandomEntrants_SelectAll() {
//        // Arrange
//        List<Entrant> entrants = Arrays.asList(testEntrants.get(0), testEntrants.get(1));
//
//        // Act
//        List<Entrant> selected = lotteryService.selectRandomEntrants(entrants, 2);
//
//        // Assert
//        assertEquals(2, selected.size());
//        assertTrue(selected.containsAll(entrants));
//    }
//
//    @Test
//    public void testCheckLotteryAvailability_Available() {
//        // Act
//        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);
//
//        // Assert
//        assertTrue(availability.isAvailable());
//        assertTrue(availability.getMessage().contains("Lottery available"));
//    }
//
//    @Test
//    public void testCheckLotteryAvailability_NotAvailableRegistration() {
//        // Arrange
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
//        testEvent.setRegEndDate(cal.getTime());
//
//        // Act
//        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);
//
//        // Assert
//        assertFalse(availability.isAvailable());
//        assertTrue(availability.getMessage().contains("Lottery not available"));
//    }
//
//    @Test
//    public void testCheckLotteryAvailability_NoEligibleEntrants() {
//        // Arrange
//        testEvent.getWaitlist().getWaitList().clear(); // Empty waitlist
//
//        // Act
//        LotteryService.LotteryAvailability availability = lotteryService.checkLotteryAvailability(testEvent);
//
//        // Assert
//        assertFalse(availability.isAvailable());
//        assertTrue(availability.getMessage().contains("No eligible entrants"));
//    }
//
//    @Test
//    public void testAutoResampleForDecline() {
//        // Arrange
//        when(mockFirestore.collection("events").document(anyString()).get())
//                .thenReturn(mockDocumentTask);
//        when(mockDocumentTask.isSuccessful()).thenReturn(true);
//        when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
//        when(mockDocumentSnapshot.exists()).thenReturn(true);
//
//        Map<String, Object> eventData = createMockEventData();
//        when(mockDocumentSnapshot.getData()).thenReturn(eventData);
//
//        when(mockFirestore.collection("events").document(anyString()).update(anyMap()))
//                .thenReturn(mockUpdateTask);
//        when(mockUpdateTask.isSuccessful()).thenReturn(true);
//
//        when(mockNotificationRepo.sendToUsers(anyList(), any(Notification.class)))
//                .thenReturn(Tasks.forResult(null));
//
//        LotteryService.LotteryCallback callback = mock(LotteryService.LotteryCallback.class);
//
//        // Act
//        lotteryService.autoResampleForDecline("test-event-123", "declined@test.com", callback);
//
//        // Assert
//        verify(callback, timeout(1000)).onLotteryCompleted(1,
//                contains("Replaced declined invitation with new entrant"));
//    }
//
//    @Test
//    public void testHandleInvitationResponse_Accept() {
//        // This test would require more complex mocking due to the nested async operations
//        // For now, we'll test the simpler methods
//
//        // Arrange
//        testEvent.getInviteList().addEntrant(testEntrants.get(0));
//
//        // This is a simplified test - in practice, you'd need to mock the entire chain
//        assertTrue(testEvent.getInviteList().containsEntrant("john@test.com"));
//    }
//
//    @Test
//    public void testConvertEntrantListToMap() {
//        // Arrange
//        EntrantList entrantList = new EntrantList();
//        entrantList.addEntrant(testEntrants.get(0));
//        entrantList.addEntrant(testEntrants.get(1));
//
//        // Act
//        Map<String, Object> result = lotteryService.convertEntrantListToMap(entrantList);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.containsKey("allEntrants"));
//        assertTrue(result.containsKey("waitList"));
//
//        List<?> allEntrants = (List<?>) result.get("allEntrants");
//        List<?> waitList = (List<?>) result.get("waitList");
//
//        assertEquals(2, allEntrants.size());
//        assertEquals(2, waitList.size());
//    }
//
//    // Helper method to create mock event data
//    private Map<String, Object> createMockEventData() {
//        Map<String, Object> eventData = new HashMap<>();
//
//        // Basic event info
//        eventData.put("eventName", "Test Event");
//        eventData.put("slots", 2L);
//        eventData.put("entrantLimit", -1L);
//
//        // Registration dates
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
//        eventData.put("regEndDate", new Timestamp(cal.getTime()));
//
//        // Waitlist data
//        Map<String, Object> waitlistData = new HashMap<>();
//        waitlistData.put("allEntrants", createEntrantListData(testEntrants));
//        waitlistData.put("waitList", createEntrantListData(testEntrants));
//        eventData.put("waitlist", waitlistData);
//
//        // Empty other lists
//        eventData.put("inviteList", new HashMap<>());
//        eventData.put("acceptedList", new HashMap<>());
//        eventData.put("declinedList", new HashMap<>());
//
//        return eventData;
//    }
//
//    // Helper method to create entrant list data
//    private List<Map<String, Object>> createEntrantListData(List<Entrant> entrants) {
//        List<Map<String, Object>> entrantList = new ArrayList<>();
//
//        for (Entrant entrant : entrants) {
//            Map<String, Object> entrantData = new HashMap<>();
//            entrantData.put("name", entrant.getName());
//            entrantData.put("email", entrant.getEmail());
//            entrantData.put("phoneNumber", entrant.getPhoneNumber());
//            entrantData.put("userType", entrant.getUserType());
//            entrantList.add(entrantData);
//        }
//
//        return entrantList;
//    }
//}



//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;
//
//import com.example.atlasevents.Event;
//import com.example.atlasevents.EntrantList;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//
//import java.util.Calendar;
//import java.util.Date;
//
///**
// * Unit tests using Mockito to mock the Event class
// */
//@RunWith(MockitoJUnitRunner.class)
//public class LotteryServiceUnitTest {
//
//    @Mock
//    private Event mockEvent;
//
//    @Before
//    public void setUp() {
//        // Setup mock behavior
//        when(mockEvent.getEntrantLimit()).thenReturn(5);
//        when(mockEvent.getAcceptedList()).thenReturn(new EntrantList());
//    }
//
//    @Test
//    public void testCalculateAvailableSlots_WithMockEvent() {
//        // Arrange
//        when(mockEvent.getEntrantLimit()).thenReturn(5);
//        when(mockEvent.getAcceptedList()).thenReturn(new EntrantList());
//
//        // Act
//        int result = calculateAvailableSlots(mockEvent);
//
//        // Assert
//        assertEquals("All slots should be available", 5, result);
//    }
//
//    @Test
//    public void testIsLotteryAvailable_WithMockEvent() {
//        // Arrange
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DAY_OF_YEAR, -1);
//        when(mockEvent.getRegEndDate()).thenReturn(cal.getTime());
//
//        // Act
//        boolean result = isLotteryAvailable(mockEvent);
//
//        // Assert
//        assertTrue("Lottery should be available", result);
//    }
//
//    // Same helper methods as above...
//    private boolean isLotteryAvailable(Event event) {
//        // Arrange
//        if (event == null || event.getRegEndDate() == null) {
//            return false;
//        }
//        // The lottery is available if the current date is AFTER the registration end date.
//        return new Date().after(event.getRegEndDate());
//    }
//
//        private int calculateAvailableSlots(Event event) {
//            if (event == null) {
//                return 0;
//            }
//            int limit = event.getEntrantLimit();
//            int acceptedCount = 0;
//
//            // Ensure the accepted list is not null before getting its size
//            if (event.getAcceptedList() != null) {
//                acceptedCount = event.getAcceptedList().size();
//            }
//
//            // The number of available slots cannot be negative
//            return Math.max(0, limit - acceptedCount);
//
//        }
//}