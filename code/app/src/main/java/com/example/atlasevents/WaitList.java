package com.example.atlasevents;

import java.util.ArrayList;

/**
 * This is a class that defines a waitlist for joining an event.
 */

public class WaitList {
    private int slots;
    private ArrayList<Entrant> waitList = new ArrayList<Entrant>();


    public WaitList(int slots) {
        this.slots = slots;
    }
    /**
     * This method returns item i from the waitlist.
     * @param i: the item in the list you want
     */
    public Entrant getEntrant(int i){
        return waitList.get(i);
    }

    /**
     * This method removes item i from the waitlist.
     * @param i: the item in the list you want to remove
     */
    public void removeEntrant(int i){
        waitList.remove(i);
    }
    public ArrayList<Entrant> getWaitList() {
        return waitList;
    }

    public void setWaitList(ArrayList<Entrant> waitList) {
        this.waitList = waitList;
    }

    public void addEntrant(Entrant entrant){
        waitList.add(entrant);
    }

    public void removeEntrant(Entrant entrant){
        waitList.remove(entrant);
    }
}
