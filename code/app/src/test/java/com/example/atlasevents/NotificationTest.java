package com.example.atlasevents;
import com.example.atlasevents.data.model.Notification;
import org.junit.Before;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link Notification} class.
 * Tests the data model for notifications including construction, getters, setters,
 * and serialization/deserialization behavior.
 *
 * @see Notification
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationTest {

    private Notification notification;
    private final String TEST_TITLE = "Test Title";
    private final String TEST_MESSAGE = "Test Message";
    private final String TEST_EVENT_ID = "event123";
    private final String TEST_ORGANIZER_EMAIL = "organizer@test.com";
    private final String TEST_EVENT_NAME = "Test Event";
    private final String TEST_GROUP_TYPE = "Waiting List";

    /**
     * Sets up test fixtures before each test method.
     * Creates a new Notification instance with test data.
     */
    @Before
    public void setUp() {
        notification = new Notification(
                TEST_TITLE,
                TEST_MESSAGE,
                TEST_EVENT_ID,
                TEST_ORGANIZER_EMAIL,
                TEST_EVENT_NAME,
                TEST_GROUP_TYPE
        );
    }

    /**
     * Tests the parameterized constructor sets all fields correctly.
     * Verifies that all provided parameters are properly assigned and
     * the notification is initialized as unread.
     */
    @Test
    public void testConstructor_SetsAllFieldsCorrectly() {
        assertEquals(TEST_TITLE, notification.getTitle());
        assertEquals(TEST_MESSAGE, notification.getMessage());
        assertEquals(TEST_EVENT_ID, notification.getEventId());
        assertEquals(TEST_ORGANIZER_EMAIL, notification.getFromOrganizeremail());
        assertEquals(TEST_EVENT_NAME, notification.getEventName());
        assertEquals(TEST_GROUP_TYPE, notification.getGroupType());
        assertFalse(notification.isRead());
    }

    /**
     * Tests the default constructor creates a valid empty notification.
     * This is primarily used by Firestore for deserialization and should
     * not throw any exceptions when called.
     */
    @Test
    public void testDefaultConstructor_CreatesValidInstance() {
        Notification emptyNotification = new Notification();
        assertNotNull(emptyNotification);
        // Default values should be null/false
        assertNull(emptyNotification.getTitle());
        assertNull(emptyNotification.getMessage());
        assertNull(emptyNotification.getEventId());
        assertNull(emptyNotification.getFromOrganizeremail());
        assertNull(emptyNotification.getEventName());
        assertNull(emptyNotification.getGroupType());
        assertFalse(emptyNotification.isRead());
    }

    /**
     * Tests notification ID getter and setter methods.
     * Verifies that the notification ID can be set and retrieved correctly.
     */
    @Test
    public void testNotificationId_GetterAndSetter_WorkCorrectly() {
        String testId = "test-notification-id";
        notification.setNotificationId(testId);
        assertEquals(testId, notification.getNotificationId());
    }

    /**
     * Tests read status getter and setter methods.
     * Verifies that the read status can be toggled and retrieved correctly.
     */
    @Test
    public void testReadStatus_GetterAndSetter_WorkCorrectly() {
        notification.setRead(true);
        assertTrue(notification.isRead());

        notification.setRead(false);
        assertFalse(notification.isRead());
    }

    /**
     * Tests createdAt timestamp getter and setter methods.
     * Verifies that the creation timestamp can be set and retrieved correctly.
     */
    @Test
    public void testCreatedAt_GetterAndSetter_WorkCorrectly() {
        Date testDate = new Date();
        notification.setCreatedAt(testDate);
        assertEquals(testDate, notification.getCreatedAt());
    }

    /**
     * Tests all string field getters and setters work correctly.
     * Verifies that each string field can be updated and retrieved properly.
     */
    @Test
    public void testAllStringFields_GetterAndSetter_WorkCorrectly() {
        // Test title
        notification.setTitle("New Title");
        assertEquals("New Title", notification.getTitle());

        // Test message
        notification.setMessage("New Message");
        assertEquals("New Message", notification.getMessage());

        // Test event ID
        notification.setEventId("new-event-id");
        assertEquals("new-event-id", notification.getEventId());

        // Test organizer email
        notification.setFromOrganizeremail("neworganizer@test.com");
        assertEquals("neworganizer@test.com", notification.getFromOrganizeremail());

        // Test event name
        notification.setEventName("New Event Name");
        assertEquals("New Event Name", notification.getEventName());

        // Test group type
        notification.setGroupType("New Group Type");
        assertEquals("New Group Type", notification.getGroupType());
    }

    /**
     * Tests that null values are handled properly in setters.
     * Verifies that setting null values doesn't cause exceptions and
     * getters return null as expected.
     */
    @Test
    public void testNullValues_AreHandledCorrectly() {
        notification.setTitle(null);
        notification.setMessage(null);
        notification.setEventId(null);
        notification.setFromOrganizeremail(null);
        notification.setEventName(null);
        notification.setGroupType(null);
        notification.setCreatedAt(null);

        assertNull(notification.getTitle());
        assertNull(notification.getMessage());
        assertNull(notification.getEventId());
        assertNull(notification.getFromOrganizeremail());
        assertNull(notification.getEventName());
        assertNull(notification.getGroupType());
        assertNull(notification.getCreatedAt());
    }
}