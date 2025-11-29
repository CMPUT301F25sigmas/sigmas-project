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

    public interface OnUserFetchedListener {
        void onUserFetched(User user);
    }

    public interface OnOrganizerFetchedListener {
        void onOrganizerFetched(Organizer organizer);
    }

    public interface OnEntrantFetchedListener {
        void onEntrantFetched(Entrant entrant);
    }

    public interface OnUserUpdatedListener {
        enum UpdateStatus { SUCCESS, EMAIL_ALREADY_USED, FAILURE }
        void onUserUpdated(UpdateStatus status);
    }

    public void addUser(@NonNull User user, @NonNull OnUserUpdatedListener listener) {

            users.put(user.getEmail(), user);
            listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS);

    }

    public void getUser(String email, OnUserFetchedListener listener) {
        listener.onUserFetched(users.get(email));
    }

    public void getOrganizer(String email, OnOrganizerFetchedListener listener) {
        User u = users.get(email);
        listener.onOrganizerFetched(u instanceof Organizer ? (Organizer) u : null);
    }

    public void getEntrant(String email, OnEntrantFetchedListener listener) {
        User u = users.get(email);
        listener.onEntrantFetched(u instanceof Entrant ? (Entrant) u : null);
    }

    public void setUser(String oldEmail, User newUser, OnUserUpdatedListener listener) {

        users.remove(oldEmail);
        users.put(newUser.getEmail(), newUser);
        listener.onUserUpdated(OnUserUpdatedListener.UpdateStatus.SUCCESS);
    }

    public void deleteUser(String email) {
        users.remove(email);
    }

    public ArrayList<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
}
