package com.example.atlasevents;

import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
import java.util.ArrayList;
import java.util.List;


import java.io.Serializable;

/**
 * This is a class that defines an Organizer which can create and host events
 * and send notifications to other Entrants.
 */

public class Organizer extends User implements Serializable {
    /*
    To do:
    Method for sending notifications to
        entrant
        EntrantLists
            waitList
            invitedList
            acceptedList
            declinedList
    */
    private NotificationRepository notificationRepository;

    public Organizer(String name, String email, String password, String phoneNumber) {
        super(name, email, password, phoneNumber);
        this.setUserType("Organizer");
        this.notificationRepository = new NotificationRepository();
    }
    
    public Organizer(){
        this.setUserType("Organizer");
        this.notificationRepository = new NotificationRepository();
    }

    public void createEvent(Event event) {
        Event myevent = new Event(this);
    }

    /**
     * Send notification to a single entrant
     */
    public void sendSingleNotification(String title, String message, Event event, Entrant entrant) {
        Notification notification = new Notification(title, message, event.getId(), this.getEmail(), event.getEventName(), "Direct message");
        notificationRepository.sendToUser(entrant.getEmail(), notification);
    }

    /**
     * Send batch notifications using the repository's convenience methods
     */
    public void sendBatchNotification(Event event, EntrantList entrantList, Integer listType) {
        String title = "";
        String message = "";
        
        switch (listType) {
            case 1: //waitlist
                title = "Waitlist Confirmation";
                message = "Thank you for joining the waitlist for: " + event.getEventName()
                        + "! We'll notify you when the lottery draw takes place.";
                break;
            case 2: //invitedList
                title = "Lottery Selection";
                message = "Congratulations! You've been selected in the lottery draw for the event: " + event.getEventName()
                        + ". Please complete your registration within 48 hours to confirm your spot.";
                break;
            case 3: //acceptedList
                title = "Registration Confirmed";
                message = "Thank you for confirming your registration! Your spot for the event: " + event.getEventName() +
                        " has been guaranteed";
                break;
            case 4: //declinedList
                title = "Invitation Declined";
                message = "Your invitation for the event: " + event.getEventName() + " has been declined successfully.";
                break;
        }
        
        // Convert EntrantList to email list and send
        List<String> emails = new ArrayList<>();
        for (int i = 0; i < entrantList.size(); i++) {
            Entrant tempEntrant = entrantList.getEntrant(i);
            if (tempEntrant != null && tempEntrant.getEmail() != null) {
                emails.add(tempEntrant.getEmail());
            }
        }
        
        Notification notification = new Notification(title, message, event.getId(), this.getEmail(), event.getEventName(), "Batch message");
        notificationRepository.sendToUsers(emails, notification);
    }

}
