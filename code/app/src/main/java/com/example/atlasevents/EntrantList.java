package com.example.atlasevents;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This is a class that defines a waitlist for joining an event.
 */

public class EntrantList implements Serializable {
  
    private ArrayList<Entrant> userList;


    public EntrantList() {
        userList = new ArrayList<Entrant>();
    }
    /**
     * This method returns item i from the waitlist.
     * @param i: the item in the list you want
     */
    public Entrant getEntrant(int i){
        return userList.get(i);
    }

    /**
     * This method removes item i from the waitlist.
     * @param i: the item in the list you want to remove
     */
    public void removeEntrant(int i){
        userList.remove(i);
    }
    public ArrayList<Entrant> getWaitList() {
        return userList;
    }

    public void setWaitList(ArrayList<Entrant> userList) {
        this.userList = userList;
    }

    public void addEntrant(Entrant entrant){
        userList.add(entrant);
    }

    public void removeEntrant(Entrant entrant){
        userList.remove(entrant);
    }

    public boolean containsEntrant(Entrant entrant){
        return userList.contains(entrant);
    }

    public boolean containsEntrant(String entrantEmail) {
        for (Entrant entrant : userList) {
            if (entrant.getEmail().equals(entrantEmail)) {
                return true;
            }
        }
        return false;
    }

    public int size(){
        return userList.size();
    }
}
