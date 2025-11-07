package com.example.atlasevents;
/**
 * Represents an entrant user who can join and participate in events.
 * <p>
 * Entrants have the ability to join event waitlists, leave waitlists, and receive
 * notifications about events. This class extends the base {@link User} class and
 * sets the user type to "Entrant".
 * </p>
 *
 * @see User
 * @see Event
 */
public class Entrant extends User{
    /*
        To Do:
        Join Waitlist Method
        Leave WaitList Method
        */
    public Entrant(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Entrant");
    }
    public Entrant(){
        this.setUserType("Entrant");
    }
    public void joinWaitlist(Event event) {
        event.addToWaitlist(this);
    }
    public void leaveWaitlist(Event event) {
        event.removeFromWaitlist(this);
    }

    /**
     * Compares this Entrant object to another object for equality.
     * <p>
     * Two Entrant objects are considered equal if they have the same email address.
     * </p>
     *
     * @param obj The object to compare with this Entrant
     * @return {@code true} if the specified object is also an Admin and has the same email; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        System.out.println("equals() called: comparing " + this.hashCode() + " with "
                + (obj instanceof Entrant ? ((Entrant)obj).hashCode() : "null/other class"));
        if (this == obj) return true; // same reference
        if (obj == null || getClass() != obj.getClass()) return false; // null or different class
        Entrant other = (Entrant) obj;
        if (this.getEmail() == null) {
            return other.getEmail() == null;
        }
        return this.getEmail().equals(other.getEmail());
    }

    /**
     * Returns a hash code value for the object.
     * <p>
     * The hash code is generated based on the email address of the entrant.
     * </p>
     * @return A hash code value for the object
     */
    @Override
    public int hashCode() {
        return getEmail() != null ? getEmail().hashCode() : 0;
    }
    public void getNotification(Event event, String message) {
        String notificationMessage = message;
        /*
            This kinda needs a textview or something in
            the XML, just holding it here
         */
    }

}
