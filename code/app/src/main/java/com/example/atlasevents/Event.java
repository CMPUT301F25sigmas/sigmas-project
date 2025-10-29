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

    //Getters
    public int getSlots() {
        return slots;
    }
    public Organizer getOrganizer() {
        return organizer;
    }
    public UserList getWaitlist() {
        return waitList;
    }
    public UserList getAcceptedList() {
        return acceptedList;
    }

    public UserList getDeclinedList() {
        return declinedList;
    }
    public UserList getInviteList(){
        return inviteList;
    }

    //Setters
    public void setSlots(int slots) {
        this.slots = slots;
    }
    public void setDeclinedList(UserList declinedList) {
        this.declinedList = declinedList;
    }

    public void setInviteList(UserList inviteList) {
        this.inviteList = inviteList;
    }
    public void setAcceptedList(UserList acceptedList) {
        this.acceptedList = acceptedList;
    }
    public void setWaitlist(UserList waitList) {
        this.waitList = waitList;
    }
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
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

    /**
     * This method adds an entrant to the waitlist
     * @param entrant the entrant to be added to waitlist
     */
    public void addToWaitlist(Entrant entrant){
        if (slots > 0) {
            waitList.addEntrant(entrant);
            slots--;
        }else{
            /* probably return an error message */
        }
    }

    /**
     * This method removes an entrant from the waitlist
     * @param entrant the entrant to be removed from waitlist
     */
    public void removeFromWaitlist(Entrant entrant){
        waitList.removeEntrant(entrant);

    }


}
