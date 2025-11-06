package com.example.atlasevents;

/**
 * Represents an administrator user with elevated privileges in the Atlas Events system.
 * <p>
 * Admins have special capabilities including sending notifications to all users,
 * removing events from the system, and removing user accounts. This class extends
 * the base {@link User} class and sets the user type to "Admin".
 * </p>
 *
 * @see User
 */

public class Admin extends User {

    public Admin(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Admin");
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
