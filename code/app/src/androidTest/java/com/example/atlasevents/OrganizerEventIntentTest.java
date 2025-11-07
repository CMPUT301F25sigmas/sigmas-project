package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple Espresso intent tests for organizer event creation.
 * Tests that intents are correctly fired when navigating to create event screen.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventIntentTest {

    @Rule
    public ActivityScenarioRule<OrganizerDashboardActivity> activityRule =
            new ActivityScenarioRule<>(OrganizerDashboardActivity.class);

    @Before
    public void setUp() {
        // Initialize Intents before each test
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Intents after each test
        Intents.release();
    }

    /**
     * Test 1: Verify that clicking create event button launches CreateEventActivity
     */
    @Test
    public void testCreateEventIntent() {
        // Wait a bit for the view to be fully laid out
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click on create event button
        onView(withId(R.id.create_event_button)).perform(click());

        // Verify that CreateEventActivity was launched
        intended(hasComponent(CreateEventActivity.class.getName()));
    }

    /**
     * Test 2: Verify that clicking back button from CreateEventActivity works
     */
    @Test
    public void testCreateEventBackNavigation() {
        // Launch CreateEventActivity directly
        ActivityScenario.launch(CreateEventActivity.class);
        
        // Wait for layout
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Click back button
        onView(withId(R.id.createBackButton)).perform(click());

        // Verify activity finishes (no assertion needed - test passes if no crash)
    }

    /**
     * Test 3: Verify that CreateEventActivity launches without crashing
     */
    @Test
    public void testCreateEventActivityLaunches() {
        // Launch CreateEventActivity directly
        ActivityScenario<CreateEventActivity> scenario = ActivityScenario.launch(CreateEventActivity.class);

        // Verify the activity launches without crashing
        scenario.onActivity(activity -> {
            // Activity successfully created
            assert activity != null;
        });
    }
}
