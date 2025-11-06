package com.example.atlasevents;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a list of entrants for an event.
 * <p>
 * This class manages a collection of {@link Entrant} objects and provides methods
 * to add, remove, retrieve, and check for entrants. It can be used for various
 * event lists such as waitlists, accepted lists, declined lists, or invite lists.
 * Implements {@link Serializable} to allow the list to be passed between activities.
 * </p>
 *
 * @see Entrant
 * @see Event
 */
public class EntrantList implements Serializable {

    /**
     * The internal list storing all entrant objects.
     */
    private ArrayList<Entrant> userList;

    /**
     * Constructs a new empty EntrantList.
     * <p>
     * Initializes the internal ArrayList to store entrants.
     * </p>
     */
    public EntrantList() {
        userList = new ArrayList<Entrant>();
    }

    /**
     * Retrieves the entrant at the specified position in the list.
     *
     * @param i The index of the entrant to retrieve
     * @return The entrant at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Entrant getEntrant(int i) {
        return userList.get(i);
    }

    /**
     * Removes the entrant at the specified position in the list.
     *
     * @param i The index of the entrant to remove
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void removeEntrant(int i) {
        userList.remove(i);
    }

    /**
     * Returns the entire list of entrants.
     *
     * @return An ArrayList containing all entrants in this list
     */
    public ArrayList<Entrant> getWaitList() {
        return userList;
    }

    /**
     * Sets the entire list of entrants, replacing the current list.
     *
     * @param userList The new ArrayList of entrants to set
     */
    public void setWaitList(ArrayList<Entrant> userList) {
        this.userList = userList;
    }

    /**
     * Adds an entrant to the end of the list.
     *
     * @param entrant The entrant to add to the list
     */
    public void addEntrant(Entrant entrant) {
        userList.add(entrant);
    }

    /**
     * Removes the first occurrence of the specified entrant from the list.
     * <p>
     * If the entrant is not present in the list, the list remains unchanged.
     * </p>
     *
     * @param entrant The entrant to remove from the list
     */
    public void removeEntrant(Entrant entrant) {
        userList.remove(entrant);
    }

    /**
     * Checks if the list contains the specified entrant.
     *
     * @param entrant The entrant to check for
     * @return {@code true} if the entrant is in the list, {@code false} otherwise
     */
    public boolean containsEntrant(Entrant entrant) {
        return userList.contains(entrant);
    }

    /**
     * Returns the number of entrants in the list.
     *
     * @return The size of the entrant list
     */
    public int size() {
        return userList.size();
    }
}