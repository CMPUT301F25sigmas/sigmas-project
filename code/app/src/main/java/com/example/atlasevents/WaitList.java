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
