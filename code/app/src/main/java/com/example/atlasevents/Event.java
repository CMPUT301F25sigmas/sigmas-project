package com.example.atlasevents;


/**
 * This is a class that defines an Event.
 */

public class Event {
    private int slots; //Number of slots available
    private Organizer organizer;
    private WaitList waitlist;
    private AcceptedList acceptedList;
    private  DeclinedList declinedList;

    public Event(Organizer organizer) {
        this.organizer = organizer;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public WaitList getWaitlist() {
        return waitlist;
    }

    public void setWaitlist(WaitList waitlist) {
        this.waitlist = waitlist;
    }

    public AcceptedList getAcceptedList() {
        return acceptedList;
    }

    public void setAcceptedList(AcceptedList acceptedList) {
        this.acceptedList = acceptedList;
    }

    public DeclinedList getDeclinedList() {
        return declinedList;
    }

    public void setDeclinedList(DeclinedList declinedList) {
        this.declinedList = declinedList;
    }
}
