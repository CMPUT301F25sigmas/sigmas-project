package com.example.atlasevents;


import java.util.ArrayList;
import java.util.Random;


/**
 * This is a class that defines an Event.
 */

public class Event {
    Random random = new Random();
    /*
        to do:
        in runLottery it currently just adds entrants to acceptedList, but I kind of want an invitedList,
        so we can remove people from the waitlist but still have access to the waitlist to invite others if someone declines

     */
    private int slots; //Number of slots available
    private Organizer organizer;
    private UserList waitList;
    private UserList inviteList;
    private UserList acceptedList;
    private UserList declinedList;

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

    public UserList getWaitlist() {
        return waitList;
    }

    public void setWaitlist(UserList waitList) {
        this.waitList = waitList;
    }

    public UserList getAcceptedList() {
        return acceptedList;
    }

    public void setAcceptedList(UserList acceptedList) {
        this.acceptedList = acceptedList;
    }

    public UserList getDeclinedList() {
        return declinedList;
    }

    public void setDeclinedList(UserList declinedList) {
        this.declinedList = declinedList;
    }

    /**
     * This method randomly selects entrants from the waitlist and moves them to the invited list.
     */
    public void runLottery(){
        while (slots > 0){
            int i = random.nextInt(slots);  //random int
            inviteList.addEntrant(waitList.getEntrant(i)); //add random user to invite list
            waitList.removeEntrant(i);  //remove user from waitlist
            slots--;    //decrement slots
        }



    }
}
