package com.example.atlasevents.data;

import androidx.annotation.NonNull;

import com.example.atlasevents.Entrant;
import com.example.atlasevents.Organizer;
import com.example.atlasevents.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FakeUserRepository extends UserRepository{

    private final Map<String, User> users = new HashMap<>();

    public FakeUserRepository() {
        // optional: prepopulate with a test user
        Organizer testOrganizer = new Organizer();
        testOrganizer.setEmail("organizer@test.com");
        testOrganizer.setName("Test Organizer");
        testOrganizer.setPassword("password");
        users.put(testOrganizer.getEmail(), testOrganizer);

        Entrant testEntrant = new Entrant();
        testEntrant.setEmail("entrant@test.com");
        testEntrant.setName("Test Entrant");
        testEntrant.setPassword("password");
        users.put(testEntrant.getEmail(), testEntrant);
    }

    // Remove the duplicate interface definitions - use the parent's interfaces
    // The parent UserRepository already defines OnUserFetchedListener, OnOrganizerFetchedListener, etc.

    @Override
    public void addUser(@NonNull User user, @NonNull OnUserUpdatedListener listener) {
        // Always return SUCCESS - this is a fake repository for testing
        users.put(user.getEmail(), user);
        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS);
    }

    @Override
    public void getUser(String name, OnUserFetchedListener listener) {
        listener.onUserFetched(users.get(name));
    }

    @Override
    public void getOrganizer(String name, OnOrganizerFetchedListener listener) {
        User u = users.get(name);
        listener.onOrganizerFetched(u instanceof Organizer ? (Organizer) u : null);
    }

    @Override
    public void getEntrant(String name, OnEntrantFetchedListener listener) {
        User u = users.get(name);
        listener.onEntrantFetched(u instanceof Entrant ? (Entrant) u : null);
    }

    @Override
    public void setUser(@NonNull String email, @NonNull User newUser, @NonNull OnUserUpdatedListener listener) {
        users.remove(email);
        users.put(newUser.getEmail(), newUser);
        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS);
    }

    @Override
    public void deleteUser(String userEmail) {
        users.remove(userEmail);
    }

    public ArrayList<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
}
