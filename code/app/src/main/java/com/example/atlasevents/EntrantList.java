package com.example.atlasevents;

import java.util.ArrayList;

/**
 * This is a class that defines a waitlist for joining an event.
 */

public class EntrantList {
  
    private ArrayList<Entrant> userList = new ArrayList<Entrant>();


    public EntrantList() {
        
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
    public int size(){
        return userList.size();
    }
}
