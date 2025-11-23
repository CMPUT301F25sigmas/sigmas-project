package com.example.atlasevents;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
import com.example.atlasevents.EntrantList;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service class for handling lottery draws for event waitlists.
 * Manages random selection of entrants from waitlists and handles notifications
 * for acceptance/decline. Supports re-drawing when entrants decline invitations.
 * Includes registration date validation for lottery availability.
 *
 * @see Event
 * @see NotificationRepository
 * @see Entrant
 */
public class LotteryService {
    private static final String TAG = "LotteryService";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepo;

    public LotteryService() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationRepo = new NotificationRepository();
    }

    /**
     * Constructor for dependency injection (for testing)
     *
     * @param db Firebase Firestore instance
     * @param notificationRepo Notification repository instance
     */
    public LotteryService(FirebaseFirestore db, NotificationRepository notificationRepo) {
        this.db = db;
        this.notificationRepo = notificationRepo;
    }

    /**
            * Callback interface for invitation responses
     */
    public interface InvitationResponseCallback {
        void onResponseSuccess(boolean accepted);
        void onResponseFailed(Exception exception);
    }

    /**
     * Draws a lottery for the specified event.
     * Randomly selects entrants from the waitlist up to the event's entrant limit,
     * considering already accepted entrants. Sends notifications to selected entrants.
     *
     * @param eventId The ID of the event to draw lottery for
     * @param callback Callback to handle the result of the lottery draw
     * @throws IllegalArgumentException if eventId is null or empty
     * @see Event
     * @see LotteryCallback
     */
    public void drawLottery(@NonNull String eventId, @NonNull LotteryCallback callback) {
        debugEventStructure(eventId);
        if (eventId.isEmpty()) {
            callback.onLotteryFailed(new IllegalArgumentException("Event ID cannot be empty"));
            return;
        }

        Log.d(TAG, "Starting lottery draw for event: " + eventId);

        // Step 1: Get event details
        getEventWithAllLists(eventId).addOnCompleteListener(eventTask -> {
            if (!eventTask.isSuccessful() || eventTask.getResult() == null) {
                Log.e(TAG, "Failed to get event details", eventTask.getException());
                callback.onLotteryFailed(eventTask.getException());
                return;
            }

            Event event = eventTask.getResult();
            if (event == null) {
                callback.onLotteryFailed(new Exception("Event not found"));
                return;
            }
            Log.d(TAG, "Event loaded: " + event.getEventName());
            Log.d(TAG, "Waitlist size from event object: " + (event.getWaitlist() != null ? event.getWaitlist().size() : "null"));
            // Step 2: Validate registration end date
            if (!isLotteryAvailable(event)) {
                callback.onLotteryFailed(new Exception("Lottery not available - registration period has not ended"));
                return;
            }

            // Step 3: Calculate available slots
            int availableSlots = calculateAvailableSlots(event);
            Log.d(TAG, "Available slots calculated: " + availableSlots);
            if (availableSlots <= 0) {
                callback.onLotteryCompleted(0, "No available slots for lottery");
                return;
            }

            // Step 4: Get waitlist and filter out already accepted/declined/invited
            EntrantList waitlist = event.getWaitlist();
            Log.d(TAG, "Waitlist before filtering: " + waitlist.size());
            List<Entrant> eligibleWaitlist = filterEligibleEntrants(waitlist, event);
            Log.d(TAG, "Eligible waitlist after filtering: " + eligibleWaitlist.size());

            if (eligibleWaitlist.isEmpty()) {
                Log.w(TAG, "No eligible entrants found. Waitlist size: " + waitlist.size());

                callback.onLotteryCompleted(0, "No eligible entrants in waitlist");
                return;
            }

            // Step 5: Randomly select entrants
            List<Entrant> selectedEntrants = selectRandomEntrants(eligibleWaitlist, availableSlots);

            if (selectedEntrants.isEmpty()) {
                callback.onLotteryCompleted(0, "No entrants selected from waitlist");
                return;
            }

            // Step 6: Move selected entrants to invited list and send notifications
            moveToInvitedAndNotify(event, selectedEntrants, callback);
        });
    }

    /**
     * Checks if lottery is available for the event based on registration end date.
     * Lottery becomes available after the registration end date has passed.
     *
     * @param event The event to check
     * @return true if lottery is available, false otherwise
     */
    public boolean isLotteryAvailable(@NonNull Event event) {
        if (event.getRegEndDate() == null) {
            Log.w(TAG, "Registration end date not set for event: " + event.getEventName());
            return false;
        }

            Date currentDate = new Date();
            Date regEndDate = event.getRegEndDate();

        // Set regEndDate to end of day (23:59:59) for comparison
        Calendar cal = Calendar.getInstance();
        cal.setTime(regEndDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endOfRegEndDate = cal.getTime();

        // Lottery is available if current date is after registration end date
        boolean available = currentDate.after(endOfRegEndDate);
        Log.d(TAG, "Lottery available for " + event.getEventName() + ": " + available +
                " (Reg End: " + regEndDate + ")");

        return available;
    }

    /**
     * Calculates the time remaining until lottery becomes available.
     *
     * @param event The event to check
     * @return Time remaining in milliseconds, or 0 if lottery is available or date invalid
     */
    public long getTimeUntilLotteryAvailable(@NonNull Event event) {
        if (event.getRegEndDate() == null) {
            return 0;
        }
        Date currentDate = new Date();
        Date regEndDate = event.getRegEndDate();

        // Set regEndDate to end of day (23:59:59)
        Calendar cal = Calendar.getInstance();
        cal.setTime(regEndDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endOfRegEndDate = cal.getTime();

        long timeRemaining = endOfRegEndDate.getTime() - currentDate.getTime();
        return Math.max(0, timeRemaining);
    }

    /**
     * Retrieves event details including all entrant lists from Firestore.
     *
     * @param eventId The event ID to retrieve
     * @return Task containing the Event object with all relevant lists
     */
    private Task<Event> getEventWithAllLists(String eventId) {
        return db.collection("events").document(eventId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        throw new Exception("Event document not found");
                    }

                    // Use enhanced parsing
                    Event event = parseEventDocument(doc);

                    // Debug: Log the list sizes
                    Log.d(TAG, "Parsed lists - Waitlist: " +
                            (event.getWaitlist() != null ? event.getWaitlist().size() : 0) +
                            ", InviteList: " +
                            (event.getInviteList() != null ? event.getInviteList().size() : 0));

                    return event;
                });
    }
    /**
     * Enhanced event parsing that handles map-based entrant lists with debugging
     */
    private Event parseEventDocument(DocumentSnapshot doc) {

        debugFirestoreDocument(doc);

        Log.d(TAG, "=== STARTING EVENT PARSING ===");

        Event event = doc.toObject(Event.class);
        if (event == null) {
            Log.d(TAG, "Event is null after toObject() - creating new Event");
            event = new Event();
        }
        event.setId(doc.getId());

        // Debug: Check what toObject() parsed
        Log.d(TAG, "After toObject() - Waitlist size: " +
                (event.getWaitlist() != null ? event.getWaitlist().size() : "null"));
        Log.d(TAG, "After toObject() - InviteList size: " +
                (event.getInviteList() != null ? event.getInviteList().size() : "null"));

        // MANUALLY PARSE INVITE LIST FROM FIRESTORE MAP
        Map<String, Object> inviteListData = (Map<String, Object>) doc.get("inviteList");
        Log.d(TAG, "Raw inviteList data from Firestore: " + inviteListData);

        if (inviteListData != null) {
            EntrantList inviteList = parseEntrantListFromFirestoreMap(inviteListData);
            event.setInviteList(inviteList);
            Log.d(TAG, "Manually parsed inviteList size: " + inviteList.size());
        } else {
            Log.d(TAG, "No inviteList data found in Firestore document");
            event.setInviteList(new EntrantList());
        }

        // Set basic fields
        if (doc.contains("eventName")) {
            event.setEventName(doc.getString("eventName"));
        }
        if (doc.contains("entrantLimit")) {
            event.setEntrantLimit(doc.getLong("entrantLimit") != null ?
                    doc.getLong("entrantLimit").intValue() : -1);
        }

        // Ensure other lists are never null
        if (event.getWaitlist() == null) {
            event.setWaitlist(new EntrantList());
        }
        if (event.getAcceptedList() == null) {
            event.setAcceptedList(new EntrantList());
        }
        if (event.getDeclinedList() == null) {
            event.setDeclinedList(new EntrantList());
        }

        Log.d(TAG, "=== FINAL EVENT STATE ===");
        Log.d(TAG, "Waitlist size: " + event.getWaitlist().size());
        Log.d(TAG, "InviteList size: " + event.getInviteList().size());
        Log.d(TAG, "AcceptedList size: " + event.getAcceptedList().size());
        Log.d(TAG, "DeclinedList size: " + event.getDeclinedList().size());
        Log.d(TAG, "=== END EVENT PARSING ===");

        return event;
    }

    /**
     * Parses entrant list from Firestore map structure where keys are emails
     */
    private EntrantList parseEntrantListFromFirestoreMap(Map<String, Object> listData) {
        Log.d(TAG, "parseEntrantListFromFirestoreMap called with: " + listData);

        EntrantList entrantList = new EntrantList();

        if (listData == null) {
            Log.d(TAG, "List data is null - returning empty list");
            return entrantList;
        }

        Log.d(TAG, "Processing " + listData.size() + " entries in inviteList");

        for (Map.Entry<String, Object> entry : listData.entrySet()) {
            String email = entry.getKey();
            Object value = entry.getValue();

            Log.d(TAG, "Processing entry - Key: " + email + ", Value type: " +
                    (value != null ? value.getClass().getSimpleName() : "null"));

            if (value instanceof Map) {
                Map<String, Object> entrantData = (Map<String, Object>) value;
                Log.d(TAG, "Entrant data for " + email + ": " + entrantData);

                Entrant entrant = new Entrant();
                entrant.setEmail(email);

                // Set fields with null checks
                if (entrantData.containsKey("name")) {
                    entrant.setName((String) entrantData.get("name"));
                }
                if (entrantData.containsKey("phoneNumber")) {
                    entrant.setPhoneNumber((String) entrantData.get("phoneNumber"));
                }
                if (entrantData.containsKey("userType")) {
                    entrant.setUserType((String) entrantData.get("userType"));
                }

                Log.d(TAG, "Created entrant: " + entrant.getEmail() + " - " + entrant.getName());
                entrantList.addEntrant(entrant);
            } else {
                Log.w(TAG, "Unexpected value type for key " + email + ": " +
                        (value != null ? value.getClass().getSimpleName() : "null"));
            }
        }

        Log.d(TAG, "Final parsed entrant list size: " + entrantList.size());
        return entrantList;
    }

    /**
     * Loads all entrant lists (waitlist, invited, accepted, declined) for the event.
     *
     * @param event The event to load lists for
     * @return Task containing event with populated lists
     */
    private Task<Event> loadAllEventLists(Event event) {

        return Tasks.forResult(event);
    }

    /**
     * Loads an entrant list from a Firestore subcollection.
     *
     * @param eventId The event ID
     * @param listName The list name (waitlist, inviteList, acceptedList, declinedList)
     * @return Task containing the EntrantList
     */
    private Task<EntrantList> loadEntrantList(String eventId, String listName) {
        return db.collection("events").document(eventId).collection(listName).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to load " + listName, task.getException());
                        return new EntrantList(); // Return empty list on failure
                    }

                    QuerySnapshot snapshot = task.getResult();
                    EntrantList entrantList = new EntrantList();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            entrantList.addEntrant(entrant);
                        }
                    }

                    Log.d(TAG, "Loaded " + entrantList.size() + " entrants from " + listName);
                    return entrantList;
                });
    }

    /**
     * Calculates available slots for the lottery draw.
     * Considers entrant limit and already accepted entrants.
     *
     * @param event The event to calculate slots for
     * @return Number of available slots
     */
    protected int calculateAvailableSlots(Event event) {
        int entrantLimit = event.getEntrantLimit();
        int acceptedCount = event.getAcceptedList() != null ? event.getAcceptedList().size() : 0;

        int availableSlots = entrantLimit - acceptedCount;
        Log.d(TAG, "Available slots: " + availableSlots + " (Limit: " + entrantLimit +
                ", Accepted: " + acceptedCount + ")");

        return Math.max(0, availableSlots);
    }

    /**
     * Filters out entrants who are already in invited, accepted, or declined lists.
     *
     * @param waitlist The original waitlist
     * @param event The event with all lists
     * @return Filtered list of eligible entrants
     */
    private List<Entrant> filterEligibleEntrants(EntrantList waitlist, Event event) {
        List<Entrant> eligible = new ArrayList<>();

        // Get all emails from other lists
        Set<String> invitedEmails = getEmailSet(event.getInviteList());
        Set<String> acceptedEmails = getEmailSet(event.getAcceptedList());
        Set<String> declinedEmails = getEmailSet(event.getDeclinedList());

        // Combine all excluded emails
        Set<String> excludedEmails = new HashSet<>();
        excludedEmails.addAll(invitedEmails);
        excludedEmails.addAll(acceptedEmails);
        excludedEmails.addAll(declinedEmails);

        // Filter waitlist
        for (int i = 0; i < waitlist.size(); i++) {
            Entrant entrant = waitlist.getEntrant(i);
            if (entrant != null && entrant.getEmail() != null) {
                String email = entrant.getEmail();
                if (!excludedEmails.contains(email)) {
                    eligible.add(entrant);
                }
            }
        }

        Log.d(TAG, "Eligible entrants: " + eligible.size() + " out of " + waitlist.size());
        return eligible;
    }

    /**
     * Gets a set of emails from an entrant list.
     *
     * @param entrantList The entrant list to extract emails from
     * @return Set of email addresses
     */
    private Set<String> getEmailSet(EntrantList entrantList) {
        Set<String> emails = new HashSet<>();
        if (entrantList != null) {
            for (int i = 0; i < entrantList.size(); i++) {
                Entrant entrant = entrantList.getEntrant(i);
                if (entrant != null && entrant.getEmail() != null) {
                    emails.add(entrant.getEmail());
                }
            }
        }
        return emails;
    }

    /**
     * Randomly selects entrants from the eligible waitlist.
     *
     * @param eligibleEntrants List of eligible entrants
     * @param count Number of entrants to select
     * @return List of randomly selected entrants
     */
    private List<Entrant> selectRandomEntrants(List<Entrant> eligibleEntrants, int count) {
        if (eligibleEntrants.size() <= count) {
            // If we have fewer or equal eligible entrants than needed, return all
            Log.d(TAG, "Selecting all " + eligibleEntrants.size() + " eligible entrants");
            return new ArrayList<>(eligibleEntrants);
        }

        // Create a shuffled copy and take the first 'count' entrants
        List<Entrant> shuffled = new ArrayList<>(eligibleEntrants);
        Collections.shuffle(shuffled);

        List<Entrant> selected = shuffled.subList(0, count);
        Log.d(TAG, "Randomly selected " + selected.size() + " entrants from " +
                eligibleEntrants.size() + " eligible");

        return selected;
    }

    /**
     * Moves selected entrants to invited list and sends notification invitations.
     *
     * @param event The event
     * @param selectedEntrants List of selected entrants
     * @param callback Lottery completion callback
     */
    private void moveToInvitedAndNotify(Event event, List<Entrant> selectedEntrants,
                                        LotteryCallback callback) {
        String eventId = event.getId();

        // Get current invite list or create new one
        EntrantList currentInviteList = event.getInviteList() != null ? event.getInviteList() : new EntrantList();

        // Add selected entrants to invite list
        for (Entrant entrant : selectedEntrants) {
            currentInviteList.addEntrant(entrant);
        }

        // Update the event document with the new invite list
        Map<String, Object> updates = new HashMap<>();
        updates.put("inviteList", convertEntrantListToMap(currentInviteList));

        db.collection("events").document(eventId)
                .update(updates)
                .addOnCompleteListener(dbTask -> {
                    if (!dbTask.isSuccessful()) {
                        Log.e(TAG, "Failed to update invite list", dbTask.getException());
                        callback.onLotteryFailed(dbTask.getException());
                        return;
                    }

                    // Send notifications to selected entrants
                    sendInvitationNotifications(event, selectedEntrants, callback);
                });
    }

    /**
     * Converts EntrantList to a Map for Firestore storage
     */
    private Map<String, Object> convertEntrantListToMap(EntrantList entrantList) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entrantList.size(); i++) {
            Entrant entrant = entrantList.getEntrant(i);
            if (entrant != null && entrant.getEmail() != null) {
                Map<String, Object> entrantMap = new HashMap<>();
                entrantMap.put("name", entrant.getName());
                entrantMap.put("email", entrant.getEmail());
                entrantMap.put("phoneNumber", entrant.getPhoneNumber());
                entrantMap.put("userType", entrant.getUserType());
                // Add other entrant fields as needed

                map.put(entrant.getEmail(), entrantMap);
            }
        }
        return map;
    }

    /**
     * Adds an entrant to the event's invited list.
     *
     * @param eventId The event ID
     * @param entrant The entrant to add
     * @return Task representing the database operation
     */
    private Task<Void> addToInvitedList(String eventId, Entrant entrant) {
        String entrantId = entrant.getEmail() != null ? entrant.getEmail() :
                String.valueOf(System.currentTimeMillis());

        return db.collection("events").document(eventId)
                .collection("inviteList")
                .document(entrantId)
                .set(entrant);
    }

    /**
     * Sends invitation notifications to selected entrants.
     *
     * @param event The event
     * @param selectedEntrants List of selected entrants
     * @param callback Lottery completion callback
     */
     private void sendInvitationNotifications (Event event, List < Entrant > selectedEntrants,
                LotteryCallback callback){
            String eventName = event.getEventName() != null ? event.getEventName() : "the event";
            String organizerEmail = event.getOrganizer() != null ?
                    event.getOrganizer().getEmail() : "Unknown Organizer";

            String title = "Event Invitation: " + eventName;
            String message = "Congratulations! You have been selected from the waitlist for " +
                    eventName + ". Please accept or decline this invitation within 24 hours.";

            // Create notification with action type
            Notification invitationNotification = new Notification(
                    title, message, event.getId(), organizerEmail, eventName, "Invitation"
            );

            // Set expiration time (24 hours from now)
            long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
            invitationNotification.setExpirationTime(expirationTime);

            List<String> selectedEmails = new ArrayList<>();
            for (Entrant entrant : selectedEntrants) {
                if (entrant.getEmail() != null) {
                    selectedEmails.add(entrant.getEmail());

                    // Schedule auto-decline for each entrant
                    scheduleAutoDecline(event.getId(), entrant.getEmail(), expirationTime);
                }
            }

            // Send notifications
            notificationRepo.sendToUsers(selectedEmails, invitationNotification)
                    .addOnCompleteListener(notificationTask -> {
                        if (notificationTask.isSuccessful()) {
                            Log.d(TAG, "Successfully sent invitations to " + selectedEmails.size() + " entrants");
                            callback.onLotteryCompleted(selectedEmails.size(),
                                    "Lottery completed. " + selectedEmails.size() + " entrants notified.");
                        } else {
                            Log.e(TAG, "Failed to send invitation notifications", notificationTask.getException());
                            callback.onLotteryCompleted(selectedEmails.size(),
                                    "Lottery completed but some notifications failed.");
                        }
                    });
        }
    /**
     * Schedules automatic decline for unanswered invitations
     */
    /**
     * Schedules automatic decline for unanswered invitations
     */
    private void scheduleAutoDecline(String eventId, String entrantEmail, long expirationTime) {
        long delay = expirationTime - System.currentTimeMillis();

        // Only schedule if delay is positive
        if (delay <= 0) {
            Log.d(TAG, "No need to schedule auto-decline - already expired");
            return;
        }

        Log.d(TAG, "Scheduling auto-decline for " + entrantEmail + " in " + delay + "ms");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if entrant is still in invite list (hasn't responded)
            getEventWithAllLists(eventId).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Event event = task.getResult();
                    EntrantList inviteList = event.getInviteList();

                    if (inviteList != null && inviteList.containsEntrant(entrantEmail)) {
                        // Auto-decline if still in invite list
                        Log.d(TAG, "Auto-declining invitation for: " + entrantEmail);
                        handleInvitationResponse(eventId, entrantEmail, false, new InvitationResponseCallback() {
                            @Override
                            public void onResponseSuccess(boolean accepted) {
                                Log.d(TAG, "Auto-decline successful for: " + entrantEmail);
                            }

                            @Override
                            public void onResponseFailed(Exception exception) {
                                Log.e(TAG, "Auto-decline failed for: " + entrantEmail, exception);
                            }
                        });

                        // Send auto-decline notification
                        sendAutoDeclineNotification(event, entrantEmail);
                    } else {
                        Log.d(TAG, "Entrant already responded: " + entrantEmail);
                    }
                } else {
                    Log.e(TAG, "Failed to check invite list status for auto-decline", task.getException());
                }
            });
        }, delay);
    }

    /**
     * Sends notification for auto-declined invitation
     */
    private void sendAutoDeclineNotification(Event event, String entrantEmail) {
        String title = "Invitation Expired: " + event.getEventName();
        String message = "Your invitation for " + event.getEventName() +
                " has expired as no response was received within 24 hours.";

        Notification autoDeclineNotification = new Notification(
                title, message, event.getId(),
                event.getOrganizer().getEmail(), event.getEventName(), "AutoDecline"
        );

        List<String> emails = Collections.singletonList(entrantEmail);
        notificationRepo.sendToUsers(emails, autoDeclineNotification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auto-decline notification sent to: " + entrantEmail);
                    } else {
                        Log.e(TAG, "Failed to send auto-decline notification", task.getException());
                    }
                });
    }
    /**
     * Handles invitation response from entrant
     *
     * @param eventId The event ID
     * @param entrantEmail The entrant's email
     * @param accepted Whether the invitation was accepted
     */
    public void handleInvitationResponse(String eventId, String entrantEmail, boolean accepted, InvitationResponseCallback callback) {
        Log.d(TAG, "Handling invitation response: " + entrantEmail + " accepted: " + accepted);

        // Get event details
        getEventWithAllLists(eventId).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.e(TAG, "Failed to get event for invitation response");
                callback.onResponseFailed(task.getException());
                return;
            }

            Event event = task.getResult();
            EntrantList inviteList = event.getInviteList();
            EntrantList acceptedList = event.getAcceptedList();
            EntrantList declinedList = event.getDeclinedList();

            // Find the entrant in invite list
            Entrant respondingEntrant = null;
            for (int i = 0; i < inviteList.size(); i++) {
                Entrant entrant = inviteList.getEntrant(i);
                if (entrant != null && entrantEmail.equals(entrant.getEmail())) {
                    respondingEntrant = entrant;
                    break;
                }
            }

            if (respondingEntrant == null) {
                Log.e(TAG, "Entrant not found in invite list: " + entrantEmail);
                return;
                //callback.onResponseFailed(new Exception("Invitation not found or already responded"));
            }

            // Remove from invite list
            inviteList.removeEntrant(respondingEntrant);

            // Add to appropriate list
            if (accepted) {
                acceptedList.addEntrant(respondingEntrant);
                sendConfirmationNotification(event, respondingEntrant, true);
            } else {
                declinedList.addEntrant(respondingEntrant);
                sendConfirmationNotification(event, respondingEntrant, false);
            }

            updateEventLists(event, new InvitationResponseCallback() {
                @Override
                public void onResponseSuccess(boolean accepted) {
                    callback.onResponseSuccess(accepted);
                }

                @Override
                public void onResponseFailed(Exception exception) {
                    callback.onResponseFailed(exception);
                }
            });
        });
    }

    /**
     * Updates all event lists in Firestore
     */
    private void updateEventLists(Event event, InvitationResponseCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("inviteList", convertEntrantListToMap(event.getInviteList()));
        updates.put("acceptedList", convertEntrantListToMap(event.getAcceptedList()));
        updates.put("declinedList", convertEntrantListToMap(event.getDeclinedList()));

        db.collection("events").document(event.getId())
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully updated event lists");
                        callback.onResponseSuccess(true);
                    } else {
                        Log.e(TAG, "Failed to update event lists", task.getException());
                        callback.onResponseFailed(task.getException());
                    }
                });
    }

    /**
     * Sends confirmation notification to entrant
     */
    private void sendConfirmationNotification(Event event, Entrant entrant, boolean accepted) {
        String status = accepted ? "accepted" : "declined";
        String title = "Invitation " + status + ": " + event.getEventName();
        String message = "You have " + status + " the invitation for " + event.getEventName();

        Notification confirmationNotification = new Notification(
                title, message, event.getId(),
                event.getOrganizer().getEmail(), event.getEventName(), "InvitationResponse"
        );

        List<String> emails = Collections.singletonList(entrant.getEmail());
        notificationRepo.sendToUsers(emails, confirmationNotification);
    }

    // ... rest of your LotteryService methods ...

        /**
                * Debug method to check event structure directly
 */
    public void debugEventStructure(String eventId) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "=== DIRECT EVENT DEBUG ===");
                    Log.d(TAG, "Document ID: " + documentSnapshot.getId());
                    Log.d(TAG, "Document exists: " + documentSnapshot.exists());

                    Map<String, Object> data = documentSnapshot.getData();
                    if (data != null) {
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            Log.d(TAG, "Field: " + entry.getKey() + " = " + entry.getValue());
                            if ("waitList".equals(entry.getKey())) {
                                Object waitList = entry.getValue();
                                if (waitList instanceof Map) {
                                    Map<String, Object> waitListMap = (Map<String, Object>) waitList;
                                    Log.d(TAG, "WaitList entries: " + waitListMap.size());
                                    for (Map.Entry<String, Object> waitEntry : waitListMap.entrySet()) {
                                        Log.d(TAG, "  WaitList entry: " + waitEntry.getKey() + " = " + waitEntry.getValue());
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Document data is null");
                    }
                    Log.d(TAG, "=== END DIRECT DEBUG ===");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to debug event structure", e));
    }

    /**
     * Debug method to check the actual Firestore document structure
     */
    private void debugFirestoreDocument(DocumentSnapshot doc) {
        Log.d(TAG, "=== FIRESTORE DOCUMENT DEBUG ===");
        Log.d(TAG, "Document ID: " + doc.getId());

        Map<String, Object> data = doc.getData();
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Log.d(TAG, "Field: " + entry.getKey() + " = " + entry.getValue());

                if ("inviteList".equals(entry.getKey()) && entry.getValue() instanceof Map) {
                    Map<String, Object> inviteListMap = (Map<String, Object>) entry.getValue();
                    Log.d(TAG, "InviteList structure:");
                    for (Map.Entry<String, Object> inviteEntry : inviteListMap.entrySet()) {
                        Log.d(TAG, "  - Key: " + inviteEntry.getKey() + ", Value: " + inviteEntry.getValue());
                        if (inviteEntry.getValue() instanceof Map) {
                            Map<String, Object> entrantMap = (Map<String, Object>) inviteEntry.getValue();
                            Log.d(TAG, "    Entrant data: " + entrantMap);
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "Document data is null");
        }
        Log.d(TAG, "=== END FIRESTORE DEBUG ===");
    }
    /**
     * Callback interface for lottery operations.
     */
    public interface LotteryCallback {
        /**
         * Called when lottery is successfully completed.
         *
         * @param entrantsSelected Number of entrants selected in the lottery
         * @param message Success message with details
         */
        void onLotteryCompleted(int entrantsSelected, String message);

        /**
         * Called when lottery operation fails.
         *
         * @param exception The exception that caused the failure
         */
        void onLotteryFailed(Exception exception);
    }
}