package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Event;
import com.example.atlasevents.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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


}
