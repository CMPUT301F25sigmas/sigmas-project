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
        }

    public void joinWaitlist(Event event) {
        event.addToWaitlist(this);
    }
    public void leaveWaitlist(Event event) {
        event.removeFromWaitlist(this);
    }


}
