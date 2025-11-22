package com.example.atlasevents;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
     * Constructor for dependency injection (useful for testing)
     *
     * @param db Firebase Firestore instance
     * @param notificationRepo Notification repository instance
     */
    public LotteryService(FirebaseFirestore db, NotificationRepository notificationRepo) {
        this.db = db;
        this.notificationRepo = notificationRepo;
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

            // Step 2: Validate registration end date
            if (!isLotteryAvailable(event)) {
                callback.onLotteryFailed(new Exception("Lottery not available - registration period has not ended"));
                return;
            }

            // Step 3: Calculate available slots
            int availableSlots = calculateAvailableSlots(event);
            if (availableSlots <= 0) {
                callback.onLotteryCompleted(0, "No available slots for lottery");
                return;
            }

            // Step 4: Get waitlist and filter out already accepted/declined/invited
            EntrantList waitlist = event.getWaitlist();
            List<Entrant> eligibleWaitlist = filterEligibleEntrants(waitlist, event);

            if (eligibleWaitlist.isEmpty()) {
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

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();
            Date regEndDate = formatter.parse(event.getRegEndDate());

            // Lottery is available if current date is after registration end date
            boolean available = currentDate.after(regEndDate);
            Log.d(TAG, "Lottery available for " + event.getEventName() + ": " + available +
                    " (Reg End: " + event.getRegEndDate() + ")");

            return available;

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing registration end date: " + event.getRegEndDate(), e);
            return false;
        }
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

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();
            Date regEndDate = formatter.parse(event.getRegEndDate());

            // Set regEndDate to end of day (23:59:59)
            Calendar cal = Calendar.getInstance();
            cal.setTime(regEndDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date endOfRegEndDate = cal.getTime();

            long timeRemaining = endOfRegEndDate.getTime() - currentDate.getTime();
            return Math.max(0, timeRemaining);

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing registration end date", e);
            return 0;
        }
    }

    /**
     * Retrieves event details including all entrant lists from Firestore.
     *
     * @param eventId The event ID to retrieve
     * @return Task containing the Event object with all relevant lists
     */
    private Task<Event> getEventWithAllLists(String eventId) {
        return db.collection("events").document(eventId).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        throw new Exception("Event document not found");
                    }

                    Event event = doc.toObject(Event.class);
                    if (event == null) {
                        throw new Exception("Failed to parse event data");
                    }
                    event.setId(doc.getId());

                    // Load all entrant lists from subcollections
                    return loadAllEventLists(event);
                });
    }

    /**
     * Loads all entrant lists (waitlist, invited, accepted, declined) for the event.
     *
     * @param event The event to load lists for
     * @return Task containing event with populated lists
     */
    private Task<Event> loadAllEventLists(Event event) {
        String eventId = event.getId();

        Task<EntrantList> waitlistTask = loadEntrantList(eventId, "waitlist");
        Task<EntrantList> inviteListTask = loadEntrantList(eventId, "inviteList");
        Task<EntrantList> acceptedTask = loadEntrantList(eventId, "acceptedList");
        Task<EntrantList> declinedTask = loadEntrantList(eventId, "declinedList");

        return Tasks.whenAll(waitlistTask, inviteListTask, acceptedTask, declinedTask)
                .continueWith(combinedTask -> {
                    event.setWaitlist(waitlistTask.getResult());
                    event.setInviteList(inviteListTask.getResult());
                    event.setAcceptedList(acceptedTask.getResult());
                    event.setDeclinedList(declinedTask.getResult());
                    return event;
                });
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
    private int calculateAvailableSlots(Event event) {
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
        List<Task<Void>> tasks = new ArrayList<>();

        // Add to invited list
        for (Entrant entrant : selectedEntrants) {
            Task<Void> addTask = addToInvitedList(eventId, entrant);
            tasks.add(addTask);
        }

        // Wait for all database operations to complete
        Tasks.whenAll(tasks).addOnCompleteListener(dbTask -> {
            if (!dbTask.isSuccessful()) {
                Log.e(TAG, "Failed to add entrants to invited list", dbTask.getException());
                callback.onLotteryFailed(dbTask.getException());
                return;
            }

            // Send notifications to selected entrants
            sendInvitationNotifications(event, selectedEntrants, callback);
        });
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
    private void sendInvitationNotifications(Event event, List<Entrant> selectedEntrants,
                                             LotteryCallback callback) {
        String eventName = event.getEventName() != null ? event.getEventName() : "the event";
        String organizerEmail = event.getOrganizer() != null ?
                event.getOrganizer().getEmail() : "Unknown Organizer";

        String title = "Event Invitation: " + eventName;
        String message = "Congratulations! You have been selected from the waitlist for " +
                eventName + ". Please accept or decline this invitation within 24 hours.";

        Notification invitationNotification = new Notification(
                title, message, event.getId(), organizerEmail, eventName, "Lottery Selection"
        );

        List<String> selectedEmails = new ArrayList<>();
        for (Entrant entrant : selectedEntrants) {
            if (entrant.getEmail() != null) {
                selectedEmails.add(entrant.getEmail());
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
                        // Still consider it successful since database operations completed
                        callback.onLotteryCompleted(selectedEmails.size(),
                                "Lottery completed but some notifications failed.");
                    }
                });
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