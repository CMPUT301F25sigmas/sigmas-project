package com.example.atlasevents.data.model;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Represents an event invitation in the application.
 * <p>
 * This class maps to Firestore documents in the invites collection.
 * Invitations are separate from notifications and are used specifically
 * for event lottery invitations that require accept/decline responses.
 * </p>
 *
 * @see InviteRepository
 */
public class Invite {
    private String inviteId;
    private String eventId;
    private String recipientEmail;
    private String eventName;
    private String organizerEmail;
    private String status; // "pending", "accepted", "declined", "expired"
    private long expirationTime;
    private Date createdAt;
    private String message;

    /**
     * Default constructor required for Firestore deserialization.
     */
    public Invite() {
        this.status = "pending";
    }

    /**
     * Constructs a new Invite with required fields.
     *
     * @param eventId The ID of the event
     * @param recipientEmail The email of the recipient
     * @param eventName The name of the event
     * @param organizerEmail The email of the organizer
     * @param expirationTime The expiration timestamp in milliseconds
     */
    public Invite(String eventId, String recipientEmail, String eventName, 
                  String organizerEmail, long expirationTime) {
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.eventName = eventName;
        this.organizerEmail = organizerEmail;
        this.expirationTime = expirationTime;
        this.status = "pending";
        this.message = "Congratulations! You have been selected from the waitlist for " + 
                      eventName + ". Please accept or decline this invitation within 24 hours.";
    }

    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public void setOrganizerEmail(String organizerEmail) {
        this.organizerEmail = organizerEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @ServerTimestamp
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Checks if the invite has expired.
     *
     * @return true if the current time is past the expiration time
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
}


