package com.example.atlasevents;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.atlasevents.Event;
import com.example.atlasevents.EntrantList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;

/**
 * Unit tests using Mockito to mock the Event class
 */
@RunWith(MockitoJUnitRunner.class)
public class LotteryServiceUnitTest {

    @Mock
    private Event mockEvent;

    @Before
    public void setUp() {
        // Setup mock behavior
        when(mockEvent.getEntrantLimit()).thenReturn(5);
        when(mockEvent.getAcceptedList()).thenReturn(new EntrantList());
    }

    @Test
    public void testCalculateAvailableSlots_WithMockEvent() {
        // Arrange
        when(mockEvent.getEntrantLimit()).thenReturn(5);
        when(mockEvent.getAcceptedList()).thenReturn(new EntrantList());

        // Act
        int result = calculateAvailableSlots(mockEvent);

        // Assert
        assertEquals("All slots should be available", 5, result);
    }

    @Test
    public void testIsLotteryAvailable_WithMockEvent() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        when(mockEvent.getRegEndDate()).thenReturn(cal.getTime());

        // Act
        boolean result = isLotteryAvailable(mockEvent);

        // Assert
        assertTrue("Lottery should be available", result);
    }

    // Same helper methods as above...
    private boolean isLotteryAvailable(Event event) {
        // Arrange
        if (event == null || event.getRegEndDate() == null) {
            return false;
        }
        // The lottery is available if the current date is AFTER the registration end date.
        return new Date().after(event.getRegEndDate());
    }

        private int calculateAvailableSlots(Event event) {
            if (event == null) {
                return 0;
            }
            int limit = event.getEntrantLimit();
            int acceptedCount = 0;

            // Ensure the accepted list is not null before getting its size
            if (event.getAcceptedList() != null) {
                acceptedCount = event.getAcceptedList().size();
            }

            // The number of available slots cannot be negative
            return Math.max(0, limit - acceptedCount);

        }
}