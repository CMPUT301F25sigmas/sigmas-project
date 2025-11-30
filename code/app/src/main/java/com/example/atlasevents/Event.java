package com.example.atlasevents;


import android.util.Log;

import com.example.atlasevents.data.EventRepository;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;


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
 * Modifications made to add lottery getters and setters to support LotteryService functionality
 * @see LotteryService
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
    private HashMap<String, GeoPoint> entrantCoords;
    private String Description;
    private String address;
    private Date date;
    private Date regStartDate;
    private Date regEndDate;
    private String time;
    private String imageUrl; // Firebase Storage path or URL
    private boolean requireGeolocation;
    private int entrantLimit = -1;
    private Date lastLotteryRun;
    private ArrayList<String> tags;
    private ArrayList<String> searchKeywords;


    public Event(){
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
        imageUrl = "";
        tags = new ArrayList<>();
        searchKeywords = new ArrayList<>();

    }
    public Event(Organizer organizer) {
        this.organizer = organizer;
        waitList = new EntrantList();
        inviteList = new EntrantList();
        acceptedList = new EntrantList();
        declinedList = new EntrantList();
        entrantCoords = new HashMap<>();
        imageUrl = "";
        tags = new ArrayList<>();
        searchKeywords = new ArrayList<>();
    }

    //Getters
    public Date getDate() {
        return date;
    }
    public String getTime() {
        return time;
    }

    public Date getRegStartDate() {
        return regStartDate;
    }

    public Date getRegEndDate() {
        return regEndDate;
    }

    public String getDateFormatted() {
        if (date == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
    public String getRegStartDateFormatted() {
        if (regStartDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(regStartDate);
    }

    public String getRegEndDateFormatted() {
        if (regEndDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(regEndDate);
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
    public Map<String, GeoPoint> getEntrantCoords() {return entrantCoords; }
    public String getEventName() { return eventName;}


    public String getId() {
        return id;
    }

    public boolean getRequireGeolocation(){return requireGeolocation;}

    public int getEntrantLimit(){return entrantLimit;}

    public String getImageUrl() {
        return imageUrl;
    }

    public ArrayList<String> getTags() {
        return tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public ArrayList<String> getSearchKeywords() {
        return searchKeywords == null ? new ArrayList<>() : new ArrayList<>(searchKeywords);
    }

    public Date getLastLotteryRun() {
        return lastLotteryRun;
    }
    //Setters
    public void setDate(Date date) {
        this.date = date;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public void setRegStartDate(Date regStartDate) {
        this.regStartDate = regStartDate;
    }
    public void setRegEndDate(Date regEndDate) {
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
    public void setEventName(String eventName) {
        this.eventName = eventName;
        refreshSearchKeywords();
    }

    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = new ArrayList<>();
        } else {
            this.tags = new ArrayList<>(tags);
        }
        refreshSearchKeywords();
    }

    public void setSearchKeywords(ArrayList<String> searchKeywords) {
        if (searchKeywords == null) {
            this.searchKeywords = new ArrayList<>();
        } else {
            this.searchKeywords = new ArrayList<>(searchKeywords);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRequireGeolocation(boolean bool){this.requireGeolocation = bool;}
    public void setEntrantLimit(int max){this.entrantLimit = max;}
    public void addToEntrantLocation(String email, GeoPoint coords) {
        if (coords != null) {
            entrantCoords.put(email, coords);
        }
    }
    public void removeFromEntrantLocation(Entrant entrant) {
        if (entrantCoords.containsKey(entrant)) {
            entrantCoords.remove(entrant);
        }
        return;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setLastLotteryRun(Date lastLotteryRun) {
        this.lastLotteryRun = lastLotteryRun;
    }

    /**
     * Builds a list of searchable prefixes from the event name and tags to support simple prefix searches.
     */
    private void refreshSearchKeywords() {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeywordPrefixes(keywords, eventName);

        if (tags != null) {
            for (String tag : tags) {
                addKeywordPrefixes(keywords, tag);
            }
        }

        if (searchKeywords == null) {
            searchKeywords = new ArrayList<>();
        } else {
            searchKeywords.clear();
        }
        searchKeywords.addAll(keywords);
    }

    private void addKeywordPrefixes(LinkedHashSet<String> keywords, String value) {
        if (value == null) {
            return;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }

        String[] pieces = normalized.split("\\s+");
        for (String piece : pieces) {
            for (int i = 2; i <= piece.length(); i++) {
                keywords.add(piece.substring(0, i));
            }
        }
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

        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date currentDate = cal.getTime();

        if (currentDate.before(regStartDate)) {
            Log.w("Registration", "Registration has not started yet");
            return false;
        }

        if (currentDate.after(regEndDate)) {
            Log.w("Registration", "Registration has ended");
            return false;
        }

        return true;
    }

    /**
     * This method adds an entrant to the waitlist
     * @param entrant the entrant to be added to waitlist
     */
    public int addToWaitlist(Entrant entrant) {
        if (!isRegistrationOpen()) {
            return -1;
        }
        if (waitList.containsEntrant(entrant)) {
            return -2;
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


}
