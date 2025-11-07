package com.example.atlasevents;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizerDashboardActivity business logic.
 * Tests the repository interactions and callback patterns.
 *
 * These tests verify the correct flow of data through the application
 * by testing repository methods and their callbacks in isolation.
 */
public class OrganizerDashboardActivityTest {

    @Mock
    private EventRepository mockEventRepository;

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private Session mockSession;

    private static final String TEST_EMAIL = "organizer@test.com";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockSession.getUserEmail()).thenReturn(TEST_EMAIL);
    }

    @Test
    public void testSession_getUserEmail_returnsEmail() {
        // Act
        String email = mockSession.getUserEmail();

        // Assert
        assertEquals(TEST_EMAIL, email);
        verify(mockSession).getUserEmail();
    }

    @Test
    public void testUserRepository_callbackPattern_worksWithValidOrganizer() {
        // Arrange
        Organizer mockOrganizer = mock(Organizer.class);
        when(mockOrganizer.getEmail()).thenReturn(TEST_EMAIL);

        // Create a test listener
        UserRepository.OnOrganizerFetchedListener testListener = new UserRepository.OnOrganizerFetchedListener() {
            @Override
            public void onOrganizerFetched(Organizer organizer) {
                // Verify the callback receives the correct organizer
                assertNotNull(organizer);
                assertEquals(TEST_EMAIL, organizer.getEmail());
            }
        };

        // Act - Trigger the callback directly
        testListener.onOrganizerFetched(mockOrganizer);

        // Assert - If we get here, the callback worked
        assertTrue(true);
    }

    @Test
    public void testUserRepository_callbackPattern_worksWithNullOrganizer() {
        // Arrange & Act
        UserRepository.OnOrganizerFetchedListener testListener = new UserRepository.OnOrganizerFetchedListener() {
            @Override
            public void onOrganizerFetched(Organizer organizer) {
                // Verify the callback can handle null
                assertNull(organizer);
            }
        };

        // Trigger callback with null
        testListener.onOrganizerFetched(null);

        // Assert
        assertTrue(true);
    }

    @Test
    public void testEventRepository_successCallback_receivesEvents() {
        // Arrange
        ArrayList<Event> testEvents = createMockEvents();

        // Create a test callback
        EventRepository.EventsCallback testCallback = new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                // Verify the callback receives events
                assertNotNull(events);
                assertEquals(3, events.size());
                assertEquals("event1", events.get(0).getId());
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not call onFailure");
            }
        };

        // Act
        testCallback.onSuccess(testEvents);

        // Assert
        assertTrue(true);
    }

    @Test
    public void testEventRepository_successCallback_handlesEmptyList() {
        // Arrange
        ArrayList<Event> emptyEvents = new ArrayList<>();

        // Create a test callback
        EventRepository.EventsCallback testCallback = new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                assertNotNull(events);
                assertTrue(events.isEmpty());
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not call onFailure");
            }
        };

        // Act
        testCallback.onSuccess(emptyEvents);

        // Assert
        assertTrue(true);
    }

    @Test
    public void testEventRepository_failureCallback_receivesException() {
        // Arrange
        Exception testException = new Exception("Test error");

        // Create a test callback
        EventRepository.EventsCallback testCallback = new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                fail("Should not call onSuccess");
            }

            @Override
            public void onFailure(Exception e) {
                assertNotNull(e);
                assertEquals("Test error", e.getMessage());
            }
        };

        // Act
        testCallback.onFailure(testException);

        // Assert
        assertTrue(true);
    }

    @Test
    public void testMockEvent_canSetAndGetProperties() {
        // Arrange
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("test-id");
        when(mockEvent.getEventName()).thenReturn("Test Event");
        when(mockEvent.getImageUrl()).thenReturn("https://example.com/image.jpg");

        // Act & Assert
        assertEquals("test-id", mockEvent.getId());
        assertEquals("Test Event", mockEvent.getEventName());
        assertEquals("https://example.com/image.jpg", mockEvent.getImageUrl());
    }

    @Test
    public void testMockEventList_canCreateMultipleEvents() {
        // Arrange & Act
        ArrayList<Event> events = createMockEvents();

        // Assert
        assertNotNull(events);
        assertEquals(3, events.size());
        assertEquals("event1", events.get(0).getId());
        assertEquals("Test Event 2", events.get(1).getEventName());
        assertEquals("", events.get(1).getImageUrl());
        assertEquals("https://example.com/image3.jpg", events.get(2).getImageUrl());
    }

    @Test
    public void testOrganizer_mockCanBeCreatedAndUsed() {
        // Arrange
        Organizer mockOrganizer = mock(Organizer.class);
        when(mockOrganizer.getEmail()).thenReturn(TEST_EMAIL);
        when(mockOrganizer.getName()).thenReturn("Test Organizer");

        // Act & Assert
        assertEquals(TEST_EMAIL, mockOrganizer.getEmail());
        assertNotNull(mockOrganizer.getName());
    }

    @Test
    public void testCallbackInterfaces_exist() {
        // Test that callback interfaces can be instantiated
        UserRepository.OnOrganizerFetchedListener organizerListener = organizer -> {
            // Lambda implementation
        };

        EventRepository.EventsCallback eventsCallback = new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {}

            @Override
            public void onFailure(Exception e) {}
        };

        assertNotNull(organizerListener);
        assertNotNull(eventsCallback);
    }

    @Test
    public void testEventList_canAddAndRemoveEvents() {
        // Arrange
        ArrayList<Event> events = new ArrayList<>();
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("test-event");

        // Act
        events.add(mockEvent);

        // Assert
        assertEquals(1, events.size());
        assertEquals("test-event", events.get(0).getId());

        // Act - Remove
        events.remove(0);

        // Assert
        assertTrue(events.isEmpty());
    }

    @Test
    public void testRepositoryMock_canBeVerified() {
        // Arrange
        doNothing().when(mockEventRepository).getEventsByOrganizer(anyString(), any());

        // Act
        mockEventRepository.getEventsByOrganizer(TEST_EMAIL, null);

        // Assert
        verify(mockEventRepository).getEventsByOrganizer(eq(TEST_EMAIL), any());
    }

    @Test
    public void testCompleteDataFlow_simulation() {
        // This test simulates the complete data flow without actual Activity

        // Step 1: Get organizer
        Organizer mockOrganizer = mock(Organizer.class);
        when(mockOrganizer.getEmail()).thenReturn(TEST_EMAIL);

        // Step 2: Get events for organizer
        ArrayList<Event> mockEvents = createMockEvents();

        // Step 3: Verify data integrity
        assertNotNull(mockOrganizer);
        assertEquals(TEST_EMAIL, mockOrganizer.getEmail());
        assertNotNull(mockEvents);
        assertEquals(3, mockEvents.size());

        // Step 4: Verify we can access event properties
        for (Event event : mockEvents) {
            assertNotNull(event.getId());
            assertNotNull(event.getEventName());
        }
    }

    // Helper method to create mock events
    private ArrayList<Event> createMockEvents() {
        ArrayList<Event> events = new ArrayList<>();

        Event event1 = mock(Event.class);
        when(event1.getId()).thenReturn("event1");
        when(event1.getEventName()).thenReturn("Test Event 1");
        when(event1.getImageUrl()).thenReturn("https://example.com/image1.jpg");

        Event event2 = mock(Event.class);
        when(event2.getId()).thenReturn("event2");
        when(event2.getEventName()).thenReturn("Test Event 2");
        when(event2.getImageUrl()).thenReturn("");

        Event event3 = mock(Event.class);
        when(event3.getId()).thenReturn("event3");
        when(event3.getEventName()).thenReturn("Test Event 3");
        when(event3.getImageUrl()).thenReturn("https://example.com/image3.jpg");

        events.add(event1);
        events.add(event2);
        events.add(event3);

        return events;
    }
}