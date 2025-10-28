package com.example.atlasevents;

import java.util.ArrayList;

/**
 * This is a class that defines a list of entrants that were invited
 * but declined joining the event.
 */

public class DeclinedList {
    private ArrayList<Entrant> declinedList = new ArrayList<Entrant>();


    public ArrayList<Entrant> getDeclinedList() {
        return declinedList;
    }

    public void setDeclinedList(ArrayList<Entrant> declinedList) {
        this.declinedList = declinedList;
    }

    public void addEntrant(Entrant entrant){
        declinedList.add(entrant);
    }

    public void removeEntrant(Entrant entrant){
        declinedList.remove(entrant);
    }
}
