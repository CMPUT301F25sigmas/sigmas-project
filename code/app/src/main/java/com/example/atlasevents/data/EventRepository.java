package com.example.atlasevents.data;

import android.util.Log;

import com.example.atlasevents.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * Handles Firebase Firestore operations for managing {@link Event} objects in the Atlas Events app.
 * <p>
 * This repository provides CRUD (Create, Read, Update, Delete) functionality and utility
 * methods for retrieving events by organizer, availability, and name.
 * </p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * EventRepository eventRepo = new EventRepository();
 * eventRepo.getAllEvents(new EventRepository.EventsCallback() {
 *     @Override
 *     public void onSuccess(ArrayList<Event> events) {
 *         // handle success
 *     }
 *
 *     @Override
 *     public void onFailure(Exception e) {
 *         // handle failure
 *     }
 * });
 * }</pre>
 */
public class EventRepository {

    /** Reference to the Firestore database. */
    private final FirebaseFirestore db;

    /**
     * Constructs a new {@code EventRepository} and initializes the Firestore instance.
     */
    public EventRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Adds a new event to the Firestore database.
     * <p>
     * A unique document ID is generated and assigned to the event before being saved.
     * </p>
     *
     * @param event the {@link Event} object to be added
     * @return a {@link Task} that resolves with the newly created event's document ID
     */
    public Task<String> addEvent(Event event) {
        DocumentReference docRef = db.collection("events").document();
        event.setId(docRef.getId()); // Save ID inside event
        return docRef.set(event)
                .continueWith(task -> docRef.getId());
    }

    /**
     * Callback interface for retrieving multiple {@link Event} objects.
     */
    public interface EventsCallback {
        /**
         * Called when events are successfully fetched.
         *
         * @param events a list of retrieved events
         */
        void onSuccess(ArrayList<Event> events);

        /**
         * Called when fetching events fails.
         *
         * @param e the exception encountered
         */
        void onFailure(Exception e);
    }

    /**
     * Callback interface for retrieving a single {@link Event} object.
     */
    public interface EventCallback {
        /**
         * Called when the event is successfully fetched.
         *
         * @param event the retrieved {@link Event} object
         */
        void onSuccess(Event event);

        /**
         * Called when fetching the event fails.
         *
         * @param e the exception encountered
         */
        void onFailure(Exception e);
    }

    /**
     * Retrieves all events stored in Firestore.
     *
     * @param callback a {@link EventsCallback} to handle success or failure
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
     * Retrieves all available events that have open registration slots.
     *
     * @param callback a {@link EventsCallback} to handle success or failure
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
     * Retrieves a single event by its document ID.
     *
     * @param eventId  the Firestore document ID of the event
     * @param callback a {@link EventCallback} to handle success or failure
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
     * Retrieves all events organized by a specific organizer.
     * <p>
     * This method iterates through all documents and filters events by the organizer's email.
     * Debug logs are included to help trace event matching.
     * </p>
     *
     * @param organizerEmail the email address of the organizer
     * @param callback        a {@link EventsCallback} to handle success or failure
     */
    public void getEventsByOrganizer(String organizerEmail, EventsCallback callback) {
        Log.d("EventRepository", "Looking for events by organizer: " + organizerEmail);

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("EventRepository", "Total events in Firebase: " + queryDocumentSnapshots.size());

                    ArrayList<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            String eventOrganizerEmail = event.getOrganizer() != null ?
                                    event.getOrganizer().getEmail() : "null";

                            Log.d("EventRepository", "Event: " + event.getEventName() +
                                    ", Organizer email: " + eventOrganizerEmail +
                                    ", Match: " + organizerEmail.equals(eventOrganizerEmail));

                            if (event.getOrganizer() != null
                                    && organizerEmail.equals(event.getOrganizer().getEmail())) {
                                events.add(event);
                                Log.d("EventRepository", "Added event: " + event.getEventName());
                            }
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
     * @param event the {@link Event} object containing updated information
     * @return a {@link Task} representing the update operation
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection("events")
                .document(event.getId())
                .set(event);
    }

    /**
     * Deletes an event from Firestore by its document ID.
     *
     * @param eventId the ID of the event to delete
     * @return a {@link Task} representing the deletion operation
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .delete();
    }

    /**
     * Searches for events by name using a prefix-based query.
     * <p>
     * This method performs a range query that finds all event names starting with the given search string.
     * </p>
     *
     * @param searchQuery the event name prefix to search for
     * @param callback     a {@link EventsCallback} to handle success or failure
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
