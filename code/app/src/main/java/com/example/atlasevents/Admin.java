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
    /**
     * Constructs a new Admin with the specified details.
     * <p>
     * Creates an admin user and automatically sets the user type to "Admin".
     * </p>
     *
     * @param name The admin's full name
     * @param email The admin's email address
     * @param password The admin's password
     * @param phoneNumber The admin's phone number
     */
    public Admin(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Admin");
    }

    /**
     * Sends a notification message to all users in the system.
     * <p>
     * This method broadcasts a notification to every registered user,
     * regardless of their user type or status.
     * </p>
     *
     * @param message The notification message to send to all users
     */
    public void sendNotification(String message) {
        // Send notification to all users
    }

    /**
     * Removes an event from the database.
     * <p>
     * Permanently deletes the specified event and all associated data
     * from the system. This action cannot be undone.
     * </p>
     *
     * @param event The event to be removed from the database
     */
    public void removeEvent(Event event) {
        // Remove event from database
    }

    /**
     * Removes a user account from the database.
     * <p>
     * Permanently deletes the specified user and all associated data
     * from the system. This action cannot be undone.
     * </p>
     *
     * @param user The user to be removed from the database
     */
    public void removeUser(User user) {
        // Remove user from database
    }
}
