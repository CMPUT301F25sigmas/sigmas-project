package com.example.atlasevents;


import android.widget.ImageView;

import com.example.atlasevents.data.EventRepository;
import com.google.type.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;


/**
 * Represents an event in the Atlas Events application.
 * <p>
 * This class manages all aspects of an event including organizer information,
 * entrant lists (waitlist, invite list, accepted, and declined), and event details
 * such as location, time, and capacity. It implements {@link Serializable} to allow
 * event objects to be passed between Android components.
 * </p>
 * <p>
 * The event maintains multiple entrant lists to track participants through different
 * stages of the registration process, and provides lottery functionality to randomly
 * select entrants from the waitlist.
 * </p>
 *
 * @see EntrantList
 * @see Organizer
 * @see Entrant
 */
public class Event implements Serializable {

    /**
     * Random number generator used for lottery selection.
     */
    Random random = new Random();

    /**
     * Repository for database operations. Marked as transient to exclude from serialization.
     */
    transient EventRepository db = new EventRepository();

    /**
     * Unique identifier for the event.
     */
    private String id;

    /**
     * Number of available slots for the event.
     */
    private int slots;

    /**
     * The organizer who created and manages this event.
     */
    private Organizer organizer;

    /**
     * Physical address or location of the event.
     */
    private String address;

    /**
     * Detailed description of the event.
     */
    private String Description;

    /**
     * Name of the event.
     */
    private String eventName;

    /**
     * List of entrants waiting for selection.
     */
    private EntrantList waitList;

    /**
     * List of entrants who have been invited to the event.
     */
    private EntrantList inviteList;

    /**
     * List of entrants who have accepted their invitation.
     */
    private EntrantList acceptedList;

    /**
     * List of entrants who have declined their invitation.
     */
    private EntrantList declinedList;

    /**
     * Start date and time of the event.
     */
    private String start;

    /**
     * End date and time of the event.
     */
    private String end;

    /**
     * Firebase Storage path or URL for the event image.
     */
    private String imageUrl;

    /**
     * Flag indicating whether geolocation is required for entrants.
     */
    private boolean requireGeolocation;

    /**
     * Maximum number of entrants allowed in the waitlist. Value of -1 indicates no limit.
     */
    private int entrantLimit = -1;


    /**
     * Default constructor that initializes all entrant lists.
     */
    public Event(){
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
    }

    /**
     * Constructor that creates an event with a specified organizer.
     * Initializes all entrant lists.
     *
     * @param organizer The organizer who will manage this event
     */
    public Event(Organizer organizer) {
        this.organizer = organizer;
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
    }

    /**
     * Gets the physical address of the event.
     *
     * @return The event address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the physical address of the event.
     *
     * @param address The event address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the description of the event.
     *
     * @return The event description
     */
    public String getDescription() {
        return Description;
    }

    /**
     * Sets the description of the event.
     *
     * @param description The event description
     */
    public void setDescription(String description) {
        Description = description;
    }

    /**
     * Gets the start date and time of the event.
     *
     * @return The event start time
     */
    public String getStart() {
        return start;
    }

    /**
     * Gets the end date and time of the event.
     *
     * @return The event end time
     */
    public String getEnd() {
        return end;
    }

    /**
     * Gets the number of available slots for the event.
     *
     * @return The number of slots
     */
    public int getSlots() {
        return slots;
    }

    /**
     * Gets the organizer of the event.
     *
     * @return The event organizer
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Gets the waitlist of entrants.
     *
     * @return The waitlist
     */
    public EntrantList getWaitlist() {
        return waitList;
    }

    /**
     * Gets the list of entrants who have accepted their invitation.
     *
     * @return The accepted list
     */
    public EntrantList getAcceptedList() {
        return acceptedList;
    }

    /**
     * Gets the list of entrants who have declined their invitation.
     *
     * @return The declined list
     */
    public EntrantList getDeclinedList() {
        return declinedList;
    }

    /**
     * Gets the list of entrants who have been invited to the event.
     *
     * @return The invite list
     */
    public EntrantList getInviteList(){
        return inviteList;
    }

    /**
     * Gets the name of the event.
     *
     * @return The event name
     */
    public String getEventName() { return eventName;}

    /**
     * Gets the unique identifier of the event.
     *
     * @return The event ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets whether geolocation is required for this event.
     *
     * @return True if geolocation is required, false otherwise
     */
    public boolean getRequireGeolocation(){return requireGeolocation;}

    /**
     * Gets the maximum number of entrants allowed in the waitlist.
     *
     * @return The entrant limit, or -1 if there is no limit
     */
    public int getEntrantLimit(){return entrantLimit;}

    /**
     * Sets the start date and time of the event.
     *
     * @param start The event start time
     */
    public void setStart(String start) {
        this.start = start;
    }

    /**
     * Sets the end date and time of the event.
     *
     * @param end The event end time
     */
    public void setEnd(String end) {
        this.end = end;
    }

    /**
     * Sets the number of available slots for the event.
     *
     * @param slots The number of slots
     */
    public void setSlots(int slots) {
        this.slots = slots;
    }

    /**
     * Sets the list of entrants who have declined their invitation.
     *
     * @param declinedList The declined list
     */
    public void setDeclinedList(EntrantList declinedList) {
        this.declinedList = declinedList;
    }

    /**
     * Sets the list of entrants who have been invited to the event.
     *
     * @param inviteList The invite list
     */
    public void setInviteList(EntrantList inviteList) {
        this.inviteList = inviteList;
    }

    /**
     * Sets the list of entrants who have accepted their invitation.
     *
     * @param acceptedList The accepted list
     */
    public void setAcceptedList(EntrantList acceptedList) {
        this.acceptedList = acceptedList;
    }

    /**
     * Sets the waitlist of entrants.
     *
     * @param waitList The waitlist
     */
    public void setWaitlist(EntrantList waitList) {
        this.waitList = waitList;
    }

    /**
     * Sets the organizer of the event.
     *
     * @param organizer The event organizer
     */
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    /**
     * Sets the name of the event.
     *
     * @param eventName The event name
     */
    public void setEventName(String eventName) {this.eventName = eventName;}

    /**
     * Sets the unique identifier of the event.
     *
     * @param id The event ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets whether geolocation is required for this event.
     *
     * @param bool True to require geolocation, false otherwise
     */
    public void setRequireGeolocation(boolean bool){this.requireGeolocation = bool;}

    /**
     * Sets the maximum number of entrants allowed in the waitlist.
     *
     * @param max The entrant limit, or -1 for no limit
     */
    public void setEntrantLimit(int max){this.entrantLimit = max;}

    /**
     * Randomly selects entrants from the waitlist and moves them to the invited list.
     * <p>
     * This method runs a lottery by randomly selecting entrants from the waitlist
     * up to the number of available slots. Selected entrants are moved from the
     * waitlist to the invite list, and the slot count is decremented for each
     * selected entrant.
     * </p>
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
     * Adds an entrant to the waitlist if space is available.
     * <p>
     * The entrant is added only if the entrant limit has not been reached.
     * If the entrant limit is -1 (no limit), the entrant is always added.
     * After adding an entrant, the entrant limit is decremented if applicable.
     * </p>
     *
     * @param entrant The entrant to be added to the waitlist
     */
    public void addToWaitlist(Entrant entrant){
        if (entrantLimit > 0) {
            waitList.addEntrant(entrant);
            entrantLimit --;
        }else if(entrantLimit == -1){
            waitList.addEntrant(entrant);
        }
    }

    /**
     * Removes an entrant from the waitlist.
     * <p>
     * If the entrant exists in the waitlist, they are removed and the entrant limit
     * is incremented (if a limit is set). If the entrant limit is -1 (no limit),
     * the limit is not modified.
     * </p>
     *
     * @param entrant The entrant to be removed from the waitlist
     */
    public void removeFromWaitlist(Entrant entrant){
        if(waitList.containsEntrant(entrant)) {
            waitList.removeEntrant(entrant);
            if(entrantLimit >= 0) { //makes sure if entrantLimit is -1 (no limit) it doesnt inc
                entrantLimit++;
            }
        }

    }


}