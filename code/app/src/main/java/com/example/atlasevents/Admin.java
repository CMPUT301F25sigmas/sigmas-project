package com.example.atlasevents;
/**
 * This is a class that defines an Admin which can send notifications to all Users
 * and remove events and Users.
 */

public class Admin extends User {

    public Admin(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
    }

    public void sendNotification(String message) {
        // Send notification to all users
    }

    public void removeEvent(Event event) {
        // Remove event from database
    }

    public void removeUser(User user) {
        // Remove user from database
    }
}
