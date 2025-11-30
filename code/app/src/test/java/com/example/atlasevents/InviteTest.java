package com.example.atlasevents;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

import com.example.atlasevents.data.model.Invite;

/**
 * Unit tests for the Invite model class
 */
public class InviteTest {

    private Invite invite;
    private String testEventId;
    private String testRecipientEmail;
    private String testEventName;
    private String testOrganizerEmail;
    private long testExpirationTime;

    @Before
    public void setUp() {
        testEventId = "event123";
        testRecipientEmail = "user@test.com";
        testEventName = "Test Event";
        testOrganizerEmail = "organizer@test.com";
        testExpirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours from now
    }

    @Test
    public void testDefaultConstructor() {
        invite = new Invite();
        assertNotNull(invite);
        assertEquals("pending", invite.getStatus());
    }

    @Test
    public void testParameterizedConstructor() {
        invite = new Invite(testEventId, testRecipientEmail, testEventName, 
                           testOrganizerEmail, testExpirationTime);
        
        assertEquals(testEventId, invite.getEventId());
        assertEquals(testRecipientEmail, invite.getRecipientEmail());
        assertEquals(testEventName, invite.getEventName());
        assertEquals(testOrganizerEmail, invite.getOrganizerEmail());
        assertEquals(testExpirationTime, invite.getExpirationTime());
        assertEquals("pending", invite.getStatus());
        assertNotNull(invite.getMessage());
        assertTrue(invite.getMessage().contains(testEventName));
    }

    @Test
    public void testGettersAndSetters() {
        invite = new Invite();
        
        invite.setInviteId("invite123");
        invite.setEventId(testEventId);
        invite.setRecipientEmail(testRecipientEmail);
        invite.setEventName(testEventName);
        invite.setOrganizerEmail(testOrganizerEmail);
        invite.setStatus("accepted");
        invite.setExpirationTime(testExpirationTime);
        invite.setMessage("Test message");
        Date testDate = new Date();
        invite.setCreatedAt(testDate);

        assertEquals("invite123", invite.getInviteId());
        assertEquals(testEventId, invite.getEventId());
        assertEquals(testRecipientEmail, invite.getRecipientEmail());
        assertEquals(testEventName, invite.getEventName());
        assertEquals(testOrganizerEmail, invite.getOrganizerEmail());
        assertEquals("accepted", invite.getStatus());
        assertEquals(testExpirationTime, invite.getExpirationTime());
        assertEquals("Test message", invite.getMessage());
        assertEquals(testDate, invite.getCreatedAt());
    }

    @Test
    public void testIsExpired_NotExpired() {
        long futureTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour from now
        invite = new Invite(testEventId, testRecipientEmail, testEventName, 
                           testOrganizerEmail, futureTime);
        
        assertFalse(invite.isExpired());
    }

    @Test
    public void testIsExpired_Expired() {
        long pastTime = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
        invite = new Invite(testEventId, testRecipientEmail, testEventName, 
                           testOrganizerEmail, pastTime);
        
        assertTrue(invite.isExpired());
    }

    @Test
    public void testIsExpired_JustExpired() {
        long currentTime = System.currentTimeMillis() - 1000; // 1 second ago
        invite = new Invite(testEventId, testRecipientEmail, testEventName, 
                           testOrganizerEmail, currentTime);
        
        assertTrue(invite.isExpired());
    }

    @Test
    public void testStatusValues() {
        invite = new Invite();
        
        invite.setStatus("pending");
        assertEquals("pending", invite.getStatus());
        
        invite.setStatus("accepted");
        assertEquals("accepted", invite.getStatus());
        
        invite.setStatus("declined");
        assertEquals("declined", invite.getStatus());
        
        invite.setStatus("expired");
        assertEquals("expired", invite.getStatus());
    }

    @Test
    public void testMessageContainsEventName() {
        invite = new Invite(testEventId, testRecipientEmail, testEventName, 
                           testOrganizerEmail, testExpirationTime);
        
        assertNotNull(invite.getMessage());
        assertTrue(invite.getMessage().contains(testEventName));
        assertTrue(invite.getMessage().contains("selected from the waitlist"));
    }

    @Test
    public void testCustomMessage() {
        invite = new Invite();
        String customMessage = "Custom invitation message";
        invite.setMessage(customMessage);
        
        assertEquals(customMessage, invite.getMessage());
    }
}

