package com.example.atlasevents;


import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;
import java.util.Random;


/**
 * This is a class that defines an Event.
 */

public class Event {

    Random random = new Random();
    EventRepository db = new EventRepository();

    private String id;
    private int slots; //Number of slots available
    private Organizer organizer;
    private String eventName;
    private EntrantList waitList;
    private EntrantList inviteList;
    private EntrantList acceptedList;
    private EntrantList declinedList;
    private String Description;
    private String address;




    public Event(Organizer organizer) {
        this.organizer = organizer;
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
        db.addEvent(this); //add event to database
    }

    //Getters
    public int getSlots() {
        return slots;
    }
    public Organizer getOrganizer() {
        return organizer;
    }
    public EntrantList getWaitlist() {
        return waitList;
    }
    public EntrantList getAcceptedList() {
        return acceptedList;
    }

    public EntrantList getDeclinedList() {
        return declinedList;
    }
    public EntrantList getInviteList(){
        return inviteList;
    }
    public String getEventName() { return eventName;}


    public String getId() {
        return id;
    }

    //Setters
    public void setSlots(int slots) {
        this.slots = slots;
    }
    public void setDeclinedList(EntrantList declinedList) {
        this.declinedList = declinedList;
    }

    public void setInviteList(EntrantList inviteList) {
        this.inviteList = inviteList;
    }
    public void setAcceptedList(EntrantList acceptedList) {
        this.acceptedList = acceptedList;
    }
    public void setWaitlist(EntrantList waitList) {
        this.waitList = waitList;
    }
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }
    public void setEventName(String eventName) {this.eventName = eventName;}

    public void setId(String id) {
        this.id = id;
    }

    /**
     * This method randomly selects entrants from the waitlist and moves them to the invited list.
     */
    public void runLottery(){
        while (slots > 0){
            int i = random.nextInt(waitList.size());  //random int from 0 to size of waitlist
            inviteList.addEntrant(waitList.getEntrant(i)); //add i'th user in waitlist to invite list
            waitList.removeEntrant(i);  //remove user from waitlist
            slots--;    //decrement slots
        }



    }

    /**
     * This method adds an entrant to the waitlist
     * @param entrant the entrant to be added to waitlist
     */
    public void addToWaitlist(Entrant entrant){
            waitList.addEntrant(entrant);
    }

    /**
     * This method removes an entrant from the waitlist
     * @param entrant the entrant to be removed from waitlist
     */
    public void removeFromWaitlist(Entrant entrant){
        if(waitList.containsEntrant(entrant)) {
            waitList.removeEntrant(entrant);
        }

    }


}
