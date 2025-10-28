package com.example.atlasevents;
/**
 * This is a class that defines an Organizer which can create and host events
 * and send notifications to other Entrants.
 */

public class Organizer extends User{
    /*
        To do:
        Method for creating Event
        Method for sending notifications to
            waitlist
            acceptedList
            declinedList
        */


    public Organizer(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
    }
    public void createEvent(Event event) {
        Event myevent = new Event(this);
    }
    public void sendNotification(String message, Event event) {
        //to do
    }



}
