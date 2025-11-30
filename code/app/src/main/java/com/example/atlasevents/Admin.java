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
     * Compares this Admin object to another object for equality.
     * <p>
     * Two Admin objects are considered equal if they have the same email address.
     * </p>
     *
     * @param obj The object to compare with this Admin
     * @return {@code true} if the specified object is also an Admin and has the same email; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        System.out.println("equals() called: comparing " + this.hashCode() + " with "
                + (obj instanceof Admin ? ((Admin) obj).hashCode() : "null/other class"));

        if (this == obj) return true; // Same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null or different class

        Admin other = (Admin) obj;
        if (getEmail() == null) {
            return other.getEmail() == null;
        }
        return getEmail().equals(other.getEmail());
    }

    /**
     * Returns a hash code value for the Admin object.
     * <p>
     * The hash code is generated based on the admin's email address.
     * This ensures consistency with the {@link #equals(Object)} method.
     * </p>
     *
     * @return A hash code value based on the admin's email, or 0 if the email is {@code null}
     */
    @Override
    public int hashCode() {
        return getEmail() != null ? getEmail().hashCode() : 0;
    }
}
