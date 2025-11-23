package com.example.atlasevents;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.atlasevents.Event;
import com.example.atlasevents.EntrantList;
import com.example.atlasevents.Organizer;
import com.example.atlasevents.LotteryService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;

/**
 * Simple instrumented tests for LotteryService
 * Tests actual Android functionality without complex mocking
 */
@RunWith(AndroidJUnit4.class)
public class LotteryInstrumentedTest {

    private LotteryService lotteryService;
    private Event testEvent;
    private final String TEST_EVENT_ID = "test-event-123";
    private final String TEST_EVENT_NAME = "Test Event";

    @Before
    public void setUp() {
        // Use the real LotteryService - it will initialize Firebase on Android
        lotteryService = new LotteryService();

        // Create test event
        testEvent = new Event();
        testEvent.setId(TEST_EVENT_ID);
        testEvent.setEventName(TEST_EVENT_NAME);
        testEvent.setEntrantLimit(5);

        Organizer organizer = new Organizer();
        organizer.setEmail("organizer@test.com");
        testEvent.setOrganizer(organizer);

        // Create empty lists
        testEvent.setWaitlist(new EntrantList());
        testEvent.setAcceptedList(new EntrantList());
        testEvent.setInviteList(new EntrantList());
        testEvent.setDeclinedList(new EntrantList());
    }

    /**
     * Tests that LotteryService can be initialized in Android context
     */
    @Test
    public void testLotteryService_InitializesInAndroidContext() {
        // Arrange
        Context context = ApplicationProvider.getApplicationContext();

        // Act & Assert - Just verify the service was created
        assertNotNull("LotteryService should be initialized", lotteryService);
        assertNotNull("Android context should be available", context);
    }

    /**
     * Tests date logic works correctly in Android environment
     */
    @Test
    public void testDateLogic_WorksInAndroid() {
        // Arrange - Set registration end date to yesterday
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        testEvent.setRegEndDate(cal.getTime());

        // Act
        boolean available = lotteryService.isLotteryAvailable(testEvent);
        int slots = lotteryService.calculateAvailableSlots(testEvent);

        // Assert
        assertTrue("Lottery should be available after registration end date", available);
        assertEquals("Should have full capacity available", 5, slots);
    }

    /**
     * Tests that event object can be created and manipulated in Android
     */
    @Test
    public void testEventObject_CreationAndManipulation() {
        // Arrange
        testEvent.setEntrantLimit(10);

        // Act
        int limit = testEvent.getEntrantLimit();
        String name = testEvent.getEventName();
        String id = testEvent.getId();

        // Assert
        assertEquals("Event limit should be set correctly", 10, limit);
        assertEquals("Event name should be set correctly", TEST_EVENT_NAME, name);
        assertEquals("Event ID should be set correctly", TEST_EVENT_ID, id);
    }
}