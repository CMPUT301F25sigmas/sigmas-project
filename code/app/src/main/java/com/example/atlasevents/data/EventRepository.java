package com.example.atlasevents.data;

import android.util.Log;

import com.example.atlasevents.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * Repository class for handling CRUD operations on {@link Event} objects in Firebase Firestore.
 * <p>
 * Provides methods to add, retrieve, update, delete, and search for events. All operations are
 * performed asynchronously and use callback interfaces to return results.
 * </p>
 */
public class EventRepository {

    /** Reference to the Firestore database instance. */
    private FirebaseFirestore db;

    /** Initializes the repository and connects to Firestore. */
    public EventRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Adds a new event to Firestore.
     *
     * @param event The {@link Event} object to be added.
     * @return A {@link Task} that resolves with the document ID of the newly added event.
     */
    public Task<String> addEvent(Event event) {
        DocumentReference docRef = db.collection("events").document();
        event.setId(docRef.getId()); // Assign Firestore document ID to the event object
        return docRef.set(event)
                .continueWith(task -> docRef.getId());
    }

    /**
     * Callback interface for operations that return a list of events.
     */
    public interface EventsCallback {
        /**
         * Called when the event retrieval operation succeeds.
         *
         * @param events A list of {@link Event} objects.
         */
        void onSuccess(ArrayList<Event> events);

        /**
         * Called when the event retrieval operation fails.
         *
         * @param e The exception thrown.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback interface for operations that return a single event.
     */
    public interface EventCallback {
        /**
         * Called when the event retrieval succeeds.
         *
         * @param event The retrieved {@link Event} object.
         */
        void onSuccess(Event event);

        /**
         * Called when the event retrieval fails.
         *
         * @param e The exception thrown.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback interface for event update operations.
     */
    public interface EventUpdateCallback {
        /**
         * Called when the event update operation completes.
         *
         * @param success True if the update succeeded, false otherwise.
         */
        void onComplete(boolean success);
    }

    /**
     * Fetches all events from Firestore.
     *
     * @param callback The {@link EventsCallback} to handle success or failure.
     */
    public void getAllEvents(EventsCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches only events that have available slots (open for registration).
     *
     * @param callback The {@link EventsCallback} to handle success or failure.
     */
    public void getAvailableEvents(EventsCallback callback) {
        db.collection("events")
                .whereGreaterThan("slots", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches a single event by its ID.
     *
     * @param eventId  The unique identifier of the event.
     * @param callback The {@link EventCallback} to handle success or failure.
     */
    public void getEventById(String eventId, EventCallback callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Event event = documentSnapshot.toObject(Event.class);
                    if (event != null) {
                        callback.onSuccess(event);
                    } else {
                        callback.onFailure(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all events created by a specific organizer.
     *
     * @param organizerEmail The email of the organizer.
     * @param callback        The {@link EventsCallback} to handle success or failure.
     */
    public void getEventsByOrganizer(String organizerEmail, EventsCallback callback) {
        db.collection("events")
                .whereEqualTo("organizer.email", organizerEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null && organizerEmail.equals(event.getOrganizer().getEmail())) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all events where a specific entrant is on the waitlist.
     *
     * @param entrantEmail The entrant’s email.
     * @param callback     The {@link EventsCallback} to handle success or failure.
     */
    public void getEventsByEntrant(String entrantEmail, EventsCallback callback) {
        Log.d("EventRepository", "Looking for events by entrant: " + entrantEmail);

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("EventRepository", "Total events in Firebase: " + queryDocumentSnapshots.size());

                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null && event.getWaitlist().containsEntrant(entrantEmail)) {
                            events.add(event);
                            Log.d("EventRepository", "Added event: " + event.getEventName());
                        }
                    }
                    Log.d("EventRepository", "Filtered events count: " + events.size());
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates an existing event in Firestore.
     *
     * @param event    The updated {@link Event} object.
     * @param callback The {@link EventUpdateCallback} to indicate success or failure.
     */
    public void updateEvent(Event event, EventUpdateCallback callback) {
        db.collection("events")
                .document(event.getId())
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onComplete(false);
                });
    }

    /**
     * Deletes an event from Firestore by its ID.
     *
     * @param eventId The unique identifier of the event to delete.
     * @return A {@link Task} that resolves when deletion is complete.
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .delete();
    }

    /**
     * Searches for events by name (case-insensitive partial match).
     *
     * @param searchQuery The string to search for in event names.
     * @param callback    The {@link EventsCallback} to handle success or failure.
     */
    public void searchEventsByName(String searchQuery, EventsCallback callback) {
        db.collection("events")
                .orderBy("eventName")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
