package com.example.atlasevents.data;

import android.util.Log;

import com.example.atlasevents.Event;
import com.example.atlasevents.utils.ImageUploader;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;

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

    /**
     * Utility for uploading and deleting images from Firebase Storage.
     */
    private ImageUploader uploader;

    /** Initializes the repository and connects to Firestore. */
    public EventRepository() {
        db = FirebaseFirestore.getInstance();
        uploader = new ImageUploader();
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
                        if (event == null) continue;

                        boolean matches = false;


                        if (event.getWaitlist() != null &&
                                event.getWaitlist().containsEntrant(entrantEmail)) {
                            matches = true;
                        }

                        if (!matches && document.contains("inviteList")) {
                            Map<String, Object> inviteMap =
                                    (Map<String, Object>) document.get("inviteList");

                            if (inviteMap != null && inviteMap.containsKey(entrantEmail)) {
                                matches = true;
                            }
                        }

                        if (!matches && document.contains("acceptedList")) {
                            Map<String, Object> acceptedMap =
                                    (Map<String, Object>) document.get("acceptedList");

                            if (acceptedMap != null && acceptedMap.containsKey(entrantEmail)) {
                                matches = true;
                            }
                        }

                        if (matches) {
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
     */
    public void deleteEvent(String eventId) {
        DocumentReference ref = db.collection("events").document(eventId);
        ref.get().onSuccessTask(documentSnapshot -> {
            Event event = documentSnapshot.toObject(Event.class);
            assert event != null;
            if (!event.getImageUrl().isEmpty()) {
                uploader.deleteImage(event.getImageUrl(), new ImageUploader.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(String error) {
                    }
                });
            }
            return ref.delete();
        });
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

    /**
     * Searches events using the precomputed searchKeywords field (name and tags prefixes).
     * The query is normalized to lowercase and must be at least two characters long.
     *
     * @param searchQuery partial search text from the UI
     * @param callback callback to deliver matching events
     */
    public void searchEventsByKeyword(String searchQuery, EventsCallback callback) {
        if (searchQuery == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String normalized = searchQuery.trim().toLowerCase(Locale.ROOT);
        String prefixQuery = searchQuery.trim();
        if (normalized.length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("events")
                .whereArrayContains("searchKeywords", normalized)
                .get()
                .addOnSuccessListener(keywordSnapshots -> {
                    LinkedHashMap<String, Event> combined = new LinkedHashMap<>();
                    for (DocumentSnapshot document : keywordSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            String key = event.getId() != null ? event.getId() : document.getId();
                            combined.put(key, event);
                        }
                    }

                    db.collection("events")
                            .orderBy("eventName")
                            .startAt(prefixQuery)
                            .endAt(prefixQuery + "\uf8ff")
                            .get()
                            .addOnSuccessListener(nameSnapshots -> {
                                for (DocumentSnapshot document : nameSnapshots) {
                                    Event event = document.toObject(Event.class);
                                    if (event != null) {
                                        String key = event.getId() != null ? event.getId() : document.getId();
                                        combined.put(key, event);
                                    }
                                }
                                callback.onSuccess(new ArrayList<>(combined.values()));
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
