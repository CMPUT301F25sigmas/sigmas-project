package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI tests for EventManageActivity lottery system.
 * Tests lottery-related UI interactions and component visibility.
 * Uses proper activity lifecycle management for reliable testing.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventManageActivityLotteryTest {

    private ActivityScenario<EventManageActivity> scenario;

    @Before
    public void setUp() {
        // Create intent with required event ID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventManageActivity.class);
        intent.putExtra(EventManageActivity.EventKey, "test-event-123");

        // Launch activity with the intent
        scenario = ActivityScenario.launch(intent);

        // Wait for activity to stabilize
        try {
            Thread.sleep(2000); // Increased wait time for Firebase data loading
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        // Properly close the activity scenario
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test 1: Verify that basic UI elements are displayed on activity launch
     */
    @Test
    public void testBasicUI_ElementsDisplayedOnLaunch() {
        // Check main header and navigation
        onView(withId(R.id.backButton)).check(matches(isDisplayed()));
        onView(withId(R.id.eventTitle)).check(matches(isDisplayed()));

        // Check event details section
        onView(withId(R.id.eventDate)).check(matches(isDisplayed()));
        onView(withId(R.id.eventLocation)).check(matches(isDisplayed()));
        onView(withId(R.id.eventPoster)).check(matches(isDisplayed()));
    }

    /**
     * Test 2: Verify lottery-specific UI components are present
     */
    @Test
    public void testLotteryUI_ComponentsPresent() {
        // Lottery action button
        onView(withId(R.id.drawLotteryButton)).check(matches(isDisplayed()));

        // Status information
        onView(withId(R.id.lotteryStatusText)).check(matches(isDisplayed()));

        // Timer card (visibility depends on registration date)
        onView(withId(R.id.lotteryTimerCard)).check(matches(isDisplayed()));

        // Progress indicator
        onView(withId(R.id.lotteryProgressBar)).check(matches(isDisplayed()));
    }

    /**
     * Test 3: Verify entrant count displays are visible
     */
    @Test
    public void testCountDisplays_AllVisible() {
        onView(withId(R.id.waitingListCount)).check(matches(isDisplayed()));
        onView(withId(R.id.chosenCount)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelledCount)).check(matches(isDisplayed()));
        onView(withId(R.id.finalEnrolledCount)).check(matches(isDisplayed()));
    }

    /**
     * Test 4: Verify list navigation buttons are present and clickable
     */
    @Test
    public void testListNavigation_ButtonsFunctional() {
        // Check all list buttons exist
        onView(withId(R.id.WaitingListButton)).check(matches(isDisplayed()));
        onView(withId(R.id.chosenButton)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelledButton)).check(matches(isDisplayed()));
        onView(withId(R.id.enrolledButton)).check(matches(isDisplayed()));

        // Test clicking each button (they should be clickable without crashing)
        onView(withId(R.id.WaitingListButton)).perform(click());
        onView(withId(R.id.chosenButton)).perform(click());
        onView(withId(R.id.cancelledButton)).perform(click());
        onView(withId(R.id.enrolledButton)).perform(click());
    }

    /**
     * Test 5: Verify action buttons are present
     */
    @Test
    public void testActionButtons_Present() {
        onView(withId(R.id.notifyButton)).check(matches(isDisplayed()));
        onView(withId(R.id.downloadButton)).check(matches(isDisplayed()));
        onView(withId(R.id.showMapButton)).check(matches(isDisplayed()));
    }

    /**
     * Test 6: Verify entrants list container is present
     */
    @Test
    public void testEntrantsList_ContainerPresent() {
        onView(withId(R.id.entrantsRecyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.waitingListViewCard)).check(matches(isDisplayed()));
    }

    /**
     * Test 7: Verify back button functionality
     */
    @Test
    public void testBackButton_Functional() {
        onView(withId(R.id.backButton)).perform(click());
        // Test passes if no crash occurs during back navigation
    }

    /**
     * Test 8: Verify download button click doesn't crash
     */
    @Test
    public void testDownloadButton_Clickable() {
        onView(withId(R.id.downloadButton)).perform(click());
        // Should not crash even if no data is available
    }

    /**
     * Test 9: Verify notify button click doesn't crash
     */
    @Test
    public void testNotifyButton_Clickable() {
        onView(withId(R.id.notifyButton)).perform(click());
        // Should not crash when clicked
    }

    /**
     * Test 10: Verify lottery button click doesn't crash
     */
    @Test
    public void testLotteryButton_Clickable() {
        onView(withId(R.id.drawLotteryButton)).perform(click());
        // Should not crash when clicked (may show dialog or be disabled)
    }
}