package com.example.atlasevents;
/**
 * This is a class that defines an Organizer which can create and host events
 * and send notifications to other Entrants.
 */

public class Organizer extends User {
    /*
        To do:
        Method for sending notifications to
            entrant
            EntrantLists
                waitList
                invitedList
                acceptedList
                declinedList
        */


    public Organizer(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Organizer");
    }

    public void createEvent(Event event) {
        Event myevent = new Event(this);
    }

    public void sendSingleNotification(String message, Event event, Entrant entrant) {
        entrant.getNotification(event, message);
    }

    public void sendBatchNotification(Event event, EntrantList entrantList, Integer listType) {
        String message = "";
        switch (listType) {
            case 1: //waitlist
                message = "Thank you for joining the waitlist for: " + event.getEventName()
                        + "! We'll notify you when the lottery draw takes place.";

            case 2: //invitedList
                message = "Congratulations! You've been selected in the lottery draw for the event: " + event.getEventName()
                        + ". Please complete your registration within 48 hours to confirm your spot.";

            case 3: //acceptedList
                message = "Thank you for confirming your registration! Your spot for the event: " + event.getEventName() +
                        " has been guaranteed";
            case 4: //declinedList
                message = "Your invitation for the event: " + event.getEventName() + " has been declined successfully.";
        }
        for (int i = 0; i < entrantList.size(); i++) {
            Entrant tempEntrant = entrantList.getEntrant(i);
            tempEntrant.getNotification(event, message);
        }
    }
}
