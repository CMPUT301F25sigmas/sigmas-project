package com.example.atlasevents;

import java.util.ArrayList;

/**
 * This is a class that defines an AcceptedList: a list of Entrants that are signed up for an event.
 */

public class AcceptedList {
    private ArrayList<Entrant> acceptedList = new ArrayList<Entrant>();


    public ArrayList<Entrant> getAcceptedList() {
        return acceptedList;
    }

    public void setAcceptedList(ArrayList<Entrant> acceptedList) {
        this.acceptedList = acceptedList;
    }

    public void addEntrant(Entrant entrant){
        acceptedList.add(entrant);
    }

    public void removeEntrant(Entrant entrant){
        acceptedList.remove(entrant);
    }
}
