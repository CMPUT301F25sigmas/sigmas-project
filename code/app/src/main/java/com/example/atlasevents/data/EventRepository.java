package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Event;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class EventRepository {
    private FirebaseFirestore db;
    public EventRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public Task<String> addEvent(Event event) {
        DocumentReference docRef = db.collection("events").document();
        event.setId(docRef.getId()); // save ID inside event
        return docRef.set(event)
                .continueWith(task -> docRef.getId());
    }
    /**
     * Callback interface for fetching multiple events
     */
    public interface EventsCallback {
        void onSuccess(ArrayList<Event> events);
        void onFailure(Exception e);
    }

    /**
     * Callback interface for fetching a single event
     */
    public interface EventCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface EventUpdateCallback {
        void onComplete(boolean success);
    }

    /**
     * Get all events from Firebase
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
     * Get available events (events that are open for registration)
     */
    public void getAvailableEvents(EventsCallback callback) {
        db.collection("events")
                .whereGreaterThan("slots", 0) // Only events with available slots
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
     * Get a single event by ID
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
     * Get events by organizer ID
     */
    public void getEventsByOrganizer(String organizerId, EventsCallback callback) {
        db.collection("events")
                .whereEqualTo("organizer.id", organizerId) // Assuming organizer has an id field
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
     * Update an existing event
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
     * Delete an event by ID
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .delete();
    }

    /**
     * Search events by name
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
