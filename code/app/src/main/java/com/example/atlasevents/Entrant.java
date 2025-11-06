package com.example.atlasevents;
/**
 * This is a class that defines an Entrant that can join events.
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
