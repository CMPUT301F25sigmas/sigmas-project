package com.example.atlasevents;

import java.io.Serializable;

/**
 * Represents an organizer who can create and host events and send notifications to entrants.
 * <p>
 * This class extends {@link User} and adds functionality specific to event organizers,
 * including creating events and sending notifications to individual entrants or groups
 * of entrants in various lists (waitlist, invite list, accepted list, declined list).
 * It implements {@link Serializable} to allow organizer objects to be passed between
 * Android components.
 * </p>
 *
 * @see User
 * @see Event
 * @see Entrant
 * @see EntrantList
 */
public class Organizer extends User implements Serializable {



    /**
     * Constructs an Organizer with the specified details.
     * <p>
     * Initializes the organizer with user information and sets the user type to "Organizer".
     * </p>
     *
     * @param name The name of the organizer
     * @param email The email address of the organizer
     * @param password The password for the organizer's account
     * @param phoneNumber The phone number of the organizer
     */
    public Organizer(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Organizer");
    }

    /**
     * Default constructor that creates an Organizer with default values.
     * <p>
     * Sets the user type to "Organizer".
     * </p>
     */
    public Organizer(){
        this.setUserType("Organizer");
    }

    /**
     * Creates a new event with this organizer as the host.
     * <p>
     * Instantiates a new {@link Event} object with this organizer assigned to it.
     * </p>
     *
     * @param event The event template to create (currently unused in implementation)
     */
    public void createEvent(Event event) {
        Event myevent = new Event(this);
    }

    /**
     * Sends a notification message to a single entrant regarding an event.
     * <p>
     * Delivers a custom message to a specific entrant about the specified event.
     * </p>
     *
     * @param message The notification message to send
     * @param event The event the notification is about
     * @param entrant The entrant who will receive the notification
     */
    public void sendSingleNotification(String message, Event event, Entrant entrant) {
        entrant.getNotification(event, message);
    }

    /**
     * Sends a batch notification to all entrants in a specific list.
     * <p>
     * Sends automated messages to all entrants in the specified list type.
     * The message content is determined by the list type:
     * </p>
     * <ul>
     *   <li><b>Type 1 (Waitlist)</b>: Confirmation of joining the waitlist</li>
     *   <li><b>Type 2 (Invite List)</b>: Lottery selection notification with registration deadline</li>
     *   <li><b>Type 3 (Accepted List)</b>: Registration confirmation</li>
     *   <li><b>Type 4 (Declined List)</b>: Invitation decline confirmation</li>
     * </ul>
     *
     * @param event The event the notification is about
     * @param entrantList The list of entrants to notify
     * @param listType Integer identifier for the list type (1=waitlist, 2=invited, 3=accepted, 4=declined)
     */
    public void sendBatchNotification(Event event, EntrantList entrantList, Integer listType) {
        String message = "";
        switch (listType) {
            case 1: //waitlist
                message = "Thank you for joining the waitlist for: " + event.getEventName()
                        + "! We'll notify you when the lottery draw takes place.";

            case 2: //invitedList
                message = "Congratulations! You've been selected in the lottery draw for the event: " + event.getEventName()
                        + ". Please complete your registration within 48 hours to confirm your spot.";

            case 3: //acceptedList
                message = "Thank you for confirming your registration! Your spot for the event: " + event.getEventName() +
                        " has been guaranteed";
            case 4: //declinedList
                message = "Your invitation for the event: " + event.getEventName() + " has been declined successfully.";
        }
        for (int i = 0; i < entrantList.size(); i++) {
            Entrant tempEntrant = entrantList.getEntrant(i);
            tempEntrant.getNotification(event, message);
        }
    }
}