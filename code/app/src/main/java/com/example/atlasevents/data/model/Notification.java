package com.example.atlasevents.data.model;
import java.util.Date;
/***
 * This class works off a client-side model, instead of a cloud based model to create notifications
 *
 * the model for this in-app notification document shpuld be  stored in:
 *  *   users/{recipientUid}/notifications/{notifId}
 *  *
 *  * Fields:
 *  * - notificationId: (optional) document id
 *  * - title: short title shown to user
 *  * - message: full message body
 *  * - eventId: associated event id (optional)
 *  * - fromOrganizerUid: uid of the organizer who created this notification
 *  * - read: whether recipient has read the notification
 *  * - createdAt: server timestamp when created
 */
public class Notification {
    private String groupType;
    private String eventName;
    private String notificationId;
    private String title;
    private String message;
    private String eventId;
    private String fromOrganizeremail;
    private boolean read;
    private Date createdAt;

    public Notification(){}
    public Notification(String title, String message, String eventId, String fromOrganizerUid, String eventName, String groupType) {
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.fromOrganizeremail = fromOrganizerUid;
        this.eventName = eventName;
        this.read = false;
        this.groupType = groupType;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getFromOrganizeremail() { return fromOrganizeremail; }
    public void setFromOrganizeremail(String fromOrganizerUid) { this.fromOrganizeremail = fromOrganizeremail; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }
}

