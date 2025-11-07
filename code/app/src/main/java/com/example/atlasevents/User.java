package com.example.atlasevents;

/**
 * Represents a generic user in the Atlas Events application.
 * <p>
 * This class serves as the base model for all types of users such as
 * organizers and entrants. It stores essential user information including
 * name, email, password, phone number, and user type.
 * </p>
 */
public class User {

    /** The name of the user. */
    private String name;

    /** The email address of the user. */
    private String email;

    /** The password of the user. */
    private String password;

    /** The phone number of the user. */
    private String phoneNumber;

    /** The type of user (e.g., "Organizer" or "Entrant"). */
    private String userType;

    /**
     * Returns the user type.
     *
     * @return the user type as a {@link String}
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the type of user.
     *
     * @param userType the new user type (e.g., "Organizer" or "Entrant")
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Default constructor required for Firestore deserialization.
     */
    public User() {
    }

    /**
     * Constructs a new {@code User} with the specified information.
     *
     * @param name        the user's name
     * @param email       the user's email
     * @param password    the user's password
     * @param phoneNumber the user's phone number
     */
    public User(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the user's name.
     *
     * @return the user's name as a {@link String}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user's email.
     *
     * @return the user's email as a {@link String}
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's password.
     *
     * @return the user's password as a {@link String}
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the user's phone number.
     *
     * @return the user's phone number as a {@link String}
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Updates the user's name.
     *
     * @param name the new name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the user's email.
     *
     * @param email the new email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Updates the user's password.
     *
     * @param password the new password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Updates the user's phone number.
     *
     * @param phoneNumber the new phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Updates multiple profile fields for this user.
     * <p>
     * Only non-null parameters will be updated, leaving existing values
     * unchanged if {@code null} is passed.
     * </p>
     *
     * @param name        the new name of the user, or {@code null} to keep existing
     * @param email       the new email of the user, or {@code null} to keep existing
     * @param password    the new password of the user, or {@code null} to keep existing
     * @param phoneNumber the new phone number of the user, or {@code null} to keep existing
     */
    public void editProfile(String name, String email, String password, String phoneNumber) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
    }
}
