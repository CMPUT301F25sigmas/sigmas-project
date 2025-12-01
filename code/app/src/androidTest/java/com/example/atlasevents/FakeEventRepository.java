package com.example.atlasevents;

import androidx.annotation.NonNull;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.example.atlasevents.data.EventRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Fake EventRepository for testing purposes.
 * Stores events in memory instead of using Firestore.
 */
public class FakeEventRepository extends EventRepository {

    private final Map<String, Event> events = new HashMap<>();
    private Runnable dataLoadedCallback;
    private CountingIdlingResource idlingResource;

    public FakeEventRepository() {
        // Initialize IdlingResource for Espresso
        this.idlingResource = new CountingIdlingResource("FakeEventRepository");
        // Prepopulate with 2 test events
        // Event 1: Has entrant@test.com in waitlist
        Event eventWithEntrant = createTestEvent("event1", "Test Event With Entrant", "organizer@test.com");
        Entrant testEntrant = new Entrant();
        testEntrant.setEmail("entrant@test.com");
        testEntrant.setName("Test Entrant");
        eventWithEntrant.getWaitlist().addEntrant(testEntrant);
        events.put(eventWithEntrant.getId(), eventWithEntrant);

        // Event 2: Does not have entrant@test.com
        Event eventWithoutEntrant = createTestEvent("event2", "Test Event Without Entrant", "organizer@test.com");
        events.put(eventWithoutEntrant.getId(), eventWithoutEntrant);
    }
    // Add this method for the test to call
    public void setDataLoadedCallback(Runnable callback) {
        this.dataLoadedCallback = callback;
    }

    // Helper method to notify when data is loaded
    private void notifyDataLoaded() {
        if (dataLoadedCallback != null) {
            dataLoadedCallback.run();
        }
    }

    /**
     * Helper method to create a test event with basic fields populated.
     */
    private Event createTestEvent(String eventId, String eventName, String organizerEmail) {
        Event event = new Event();
        event.setId(eventId);
        event.setEventName(eventName);
        event.setSlots(10);
        event.setDescription("Test event description");
        event.setAddress("123 Test Street");
        
        // Add tags for search functionality
        ArrayList<String> tags = new ArrayList<>();
        tags.add("sports");
        tags.add("test");
        event.setTags(tags);
        
        // Set regEndDate to a future date so events pass eligibility filter
        java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 86400000L); // 1 day in future
        event.setRegEndDate(futureDate);
        
        Organizer organizer = new Organizer();
        organizer.setEmail(organizerEmail);
        organizer.setName("Test Organizer");
        event.setOrganizer(organizer);
        
        // Initialize all lists
        event.setWaitlist(new EntrantList());
        event.setInviteList(new EntrantList());
        event.setAcceptedList(new EntrantList());
        event.setDeclinedList(new EntrantList());
        
        return event;
    }

    @Override
    public Task<String> addEvent(Event event) {
        String eventId = UUID.randomUUID().toString();
        event.setId(eventId);
        events.put(eventId, event);
        return Tasks.forResult(eventId);
    }

    @Override
    public void getAllEvents(EventsCallback callback) {
        try {
            callback.onSuccess(new ArrayList<>(events.values()));
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void getAvailableEvents(EventsCallback callback) {
        try {
            ArrayList<Event> availableEvents = new ArrayList<>();
            for (Event event : events.values()) {
                if (event.getSlots() > 0) {
                    availableEvents.add(event);
                }
            }
            callback.onSuccess(availableEvents);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void getEventById(String eventId, EventCallback callback) {
        try {
            Event event = events.get(eventId);
            if (event != null) {
                callback.onSuccess(event);
            } else {
                callback.onFailure(new Exception("Event not found"));
            }
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void getEventsByOrganizer(String organizerEmail, EventsCallback callback) {
        try {
            ArrayList<Event> organizerEvents = new ArrayList<>();
            for (Event event : events.values()) {
                if (event.getOrganizer() != null && 
                    organizerEmail.equals(event.getOrganizer().getEmail())) {
                    organizerEvents.add(event);
                }
            }
            callback.onSuccess(organizerEvents);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void getEventsByEntrant(String entrantEmail, EventsCallback callback) {

        idlingResource.increment(); // Start counting
        try {
            ArrayList<Event> entrantEvents = new ArrayList<>();
            for (Event event : events.values()) {
                boolean matches = false;

                // Check waitlist
                if (event.getWaitlist() != null && 
                    event.getWaitlist().containsEntrant(entrantEmail)) {
                    matches = true;
                }

                // Check invite list
                if (!matches && event.getInviteList() != null && 
                    event.getInviteList().containsEntrant(entrantEmail)) {
                    matches = true;
                }

                // Check accepted list
                if (!matches && event.getAcceptedList() != null && 
                    event.getAcceptedList().containsEntrant(entrantEmail)) {
                    matches = true;
                }

                if (matches) {
                    entrantEvents.add(event);
                }
            }
            callback.onSuccess(entrantEvents);
            notifyDataLoaded();
        } catch (Exception e) {
            callback.onFailure(e);
        } finally {
            idlingResource.decrement(); // Stop counting
        }
    }

    @Override
    public void updateEvent(Event event, EventUpdateCallback callback) {
        try {
            if (events.containsKey(event.getId())) {
                events.put(event.getId(), event);
                if (callback != null) {
                    callback.onComplete(true);
                }
            } else {
                if (callback != null) {
                    callback.onComplete(false);
                }
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onComplete(false);
            }
        }
    }

    @Override
    public void deleteEvent(String eventId) {
        events.remove(eventId);
    }

    @Override
    public void searchEventsByName(String searchQuery, EventsCallback callback) {
        try {
            ArrayList<Event> matchingEvents = new ArrayList<>();
            String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
            for (Event event : events.values()) {
                if (event.getEventName() != null && 
                    event.getEventName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    matchingEvents.add(event);
                }
            }
            callback.onSuccess(matchingEvents);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void searchEventsByKeyword(String searchQuery, EventsCallback callback) {
        if (searchQuery == null || searchQuery.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        try {
            String normalized = searchQuery.trim().toLowerCase(Locale.ROOT);
            ArrayList<Event> matchingEvents = new ArrayList<>();
            
            for (Event event : events.values()) {
                boolean matches = false;

                // Check search keywords
                if (event.getSearchKeywords() != null) {
                    for (String keyword : event.getSearchKeywords()) {
                        if (keyword.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                            matches = true;
                            break;
                        }
                    }
                }

                // Check event name
                if (!matches && event.getEventName() != null && 
                    event.getEventName().toLowerCase(Locale.ROOT).startsWith(normalized)) {
                    matches = true;
                }

                if (matches) {
                    matchingEvents.add(event);
                }
            }
            callback.onSuccess(matchingEvents);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    /**
     * Helper method to add a test event directly (useful for test setup).
     *
     * @param event The event to add
     */
    public void addTestEvent(Event event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(UUID.randomUUID().toString());
        }
        events.put(event.getId(), event);
    }

    /**
     * Helper method to clear all events (useful for test cleanup).
     */
    public void clearAllEvents() {
        events.clear();
    }

    /**
     * Helper method to get an event by ID (synchronous, for testing).
     *
     * @param eventId The event ID
     * @return The event, or null if not found
     */
    public Event getEventByIdSync(String eventId) {
        return events.get(eventId);
    }
}
