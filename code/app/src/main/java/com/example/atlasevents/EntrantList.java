package com.example.atlasevents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EntrantList implements Serializable {

    private ArrayList<Entrant> userList;

    public EntrantList() {
        userList = new ArrayList<>();
    }

    public Entrant getEntrant(int i) {
        return userList.get(i);
    }

    public void removeEntrant(int i) {
        userList.remove(i);
    }

    /**
     * Returns all entrants as a List. Safe for UI display.
     */
    public List<Entrant> getAllEntrants() {
        return new ArrayList<>(userList);
    }

    /**
     * Deprecated getter for backward compatibility
     */
    @Deprecated
    public ArrayList<Entrant> getWaitList() {
        return userList;
    }

    public void setWaitList(ArrayList<Entrant> userList) {
        this.userList = userList != null ? userList : new ArrayList<>();
    }

    public void addEntrant(Entrant entrant) {
        if (entrant != null) {
            userList.add(entrant);
        }
    }

    public void removeEntrant(Entrant entrant) {
        userList.remove(entrant);
    }

    public boolean containsEntrant(Entrant entrant) {
        return userList.contains(entrant);
    }

    public boolean containsEntrant(String entrantEmail) {
        if (entrantEmail == null) return false;
        for (Entrant entrant : userList) {
            if (entrantEmail.equals(entrant.getEmail())) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return userList.size();
    }
}
