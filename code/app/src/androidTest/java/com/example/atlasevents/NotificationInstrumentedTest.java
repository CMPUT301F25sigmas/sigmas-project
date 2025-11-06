package com.example.atlasevents;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
@RunWith(AndroidJUnit4.class)
public class NotificationInstrumentedTest {

    private NotificationRepository notificationRepository;

    @Before
    public void setUp() {
        notificationRepository = new NotificationRepository();
    }

    @Test
    public void testRepositoryInitialization() {
        assertNotNull(notificationRepository);
        // The repository should be initialized with Firebase instance
    }

    @Test
    public void testNotificationCreation() {
        Notification notification = new Notification("Test Title", "Test Message", "test-event-123", "organizer@test.com", "test", "Instrument");

        assertEquals("Test Title", notification.getTitle());
        assertEquals("Test Message", notification.getMessage());
        assertEquals("test-event-123", notification.getEventId());
        assertEquals("organizer@test.com", notification.getFromOrganizeremail());
    }

    @After
    public void tearDown() {
        // Clean up any test data if needed
    }
}