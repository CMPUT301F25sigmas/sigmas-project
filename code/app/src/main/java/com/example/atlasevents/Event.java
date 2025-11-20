package com.example.atlasevents;


import android.util.Log;
import android.widget.ImageView;

import com.example.atlasevents.data.EventRepository;
import com.google.type.DateTime;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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

    Random random = new Random();
    transient EventRepository db = new EventRepository();

    private String id;
    private int slots; //Number of slots available
    private Organizer organizer;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    private String eventName;
    private EntrantList waitList;
    private EntrantList inviteList;
    private EntrantList acceptedList;
    private EntrantList declinedList;
    private String Description;
    private String address;
    private String date;
    private String regStartDate;
    private String regEndDate;
    private String time;
    private String imageUrl; // Firebase Storage path or URL
    private boolean requireGeolocation;
    private int entrantLimit = -1;


    public Event(){
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
        imageUrl = "";
    }
    public Event(Organizer organizer) {
        this.organizer = organizer;
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
        imageUrl = "";
    }

    //Getters
    public String getDate() {
        return date;
    }
    public String getTime() {
        return time;
    }

    public String getRegStartDate() {
        return regStartDate;
    }

    public String getRegEndDate() {
        return regEndDate;
    }

    public int getSlots() {
        return slots;
    }
    public Organizer getOrganizer() {
        return organizer;
    }
    public EntrantList getWaitlist() {
        return waitList;
    }
    public EntrantList getAcceptedList() {
        return acceptedList;
    }

    public EntrantList getDeclinedList() {
        return declinedList;
    }
    public EntrantList getInviteList(){
        return inviteList;
    }
    public String getEventName() { return eventName;}


    public String getId() {
        return id;
    }

    public boolean getRequireGeolocation(){return requireGeolocation;}

    public int getEntrantLimit(){return entrantLimit;}

    public String getImageUrl() {
        return imageUrl;
    }

    //Setters
    public void setDate(String start) {
        this.date = start;
    }
    public void setTime(String end) {
        this.time = end;
    }
    public void setRegStartDate(String regStartDate) {
        this.regStartDate = regStartDate;
    }
    public void setRegEndDate(String regEndDate) {
        this.regEndDate = regEndDate;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }
    public void setDeclinedList(EntrantList declinedList) {
        this.declinedList = declinedList;
    }

    public void setInviteList(EntrantList inviteList) {
        this.inviteList = inviteList;
    }
    public void setAcceptedList(EntrantList acceptedList) {
        this.acceptedList = acceptedList;
    }
    public void setWaitlist(EntrantList waitList) {
        this.waitList = waitList;
    }
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }
    public void setEventName(String eventName) {this.eventName = eventName;}

    public void setId(String id) {
        this.id = id;
    }

    public void setRequireGeolocation(boolean bool){this.requireGeolocation = bool;}
    public void setEntrantLimit(int max){this.entrantLimit = max;}

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * This method randomly selects entrants from the waitlist and moves them to the invited list.
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
     * Validates if registration is currently open based on registration dates
     * @return true if registration is open, false otherwise
     */
    public boolean isRegistrationOpen() {
        if (regStartDate == null || regEndDate == null) {
            Log.w("Registration", "Registration dates not set");
            return false;
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();
            Date startDate = formatter.parse(regStartDate);
            Date endDate = formatter.parse(regEndDate);

            if (currentDate.before(startDate)) {
                Log.w("Registration", "Registration has not started yet");
                return false;
            }

            if (currentDate.after(endDate)) {
                Log.w("Registration", "Registration has ended");
                return false;
            }

            return true;

        } catch (ParseException e) {
            Log.e("Registration", "Error parsing dates");
            return false;
        }
    }

    /**
     * This method adds an entrant to the waitlist
     * @param entrant the entrant to be added to waitlist
     */
    public int addToWaitlist(Entrant entrant) {
        if (!isRegistrationOpen()) {
            return -1;
        }
        int currentSize = waitList.size();
        if (entrantLimit == -1) {
            waitList.addEntrant(entrant);
            return 1;
        }
        if (currentSize < entrantLimit) {
            waitList.addEntrant(entrant);
            return 1;
        } else {
            Log.w("Waitlist", "Cannot add entrant: waitlist limit reached");
            return 0;
        }
    }

    /**
     * This method removes an entrant from the waitlist
     * @param entrant the entrant to be removed from waitlist
     */
    public void removeFromWaitlist(Entrant entrant){
        if(waitList.containsEntrant(entrant)) {
            waitList.removeEntrant(entrant);
        }
    }

    /**
     * This method converts string date to timestamp
     *
     * @param event
     * @return date
     */
    public static long getEventTimestamp(Event event) {
        try {
            String dateTime = event.getDate() + " " + event.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTime);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
