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


    public void runLottery(){
        ArrayList<Entrant> lotteryList = waitlist.getWaitList();
        while (slots > 0){
            int i = random.nextInt(slots);
            acceptedList.addEntrant(lotteryList.get(i)); //for now i'll just add them to accepted list, but I do think we need a new list :)
            slots--;
            waitlist.removeEntrant(i);
        }



    }
}
