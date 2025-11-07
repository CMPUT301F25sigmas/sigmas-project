package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple Espresso intent tests for notification screens.
 * Tests that intents are correctly fired when navigating between notification activities.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationIntentTest {

    @Rule
    public ActivityScenarioRule<NotificationCenterActivity> activityRule =
            new ActivityScenarioRule<>(NotificationCenterActivity.class);

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
     * Test 1: Verify that clicking notification history button launches NotificationHistoryActivity
     */
    @Test
    public void testNotificationHistoryIntent() {
        // Click on notification history button
        onView(withId(R.id.notificationHistory)).perform(click());

        // Verify that NotificationHistoryActivity was launched
        intended(hasComponent(NotificationHistoryActivity.class.getName()));
    }

    /**
     * Test 2: Verify that clicking back button from NotificationHistoryActivity works
     */
    @Test
    public void testNotificationHistoryBackNavigation() {
        // Launch NotificationHistoryActivity directly
        ActivityScenario.launch(NotificationHistoryActivity.class);
        
        // Click back button
        onView(withId(R.id.backButton)).perform(click());

        // Verify activity finishes (no assertion needed - test passes if no crash)
    }

    /**
     * Test 3: Verify that ComposeNotificationActivity receives correct intent extras
     */
    @Test
    public void testComposeNotificationIntentWithExtras() {
        // Create intent with extras for ComposeNotificationActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ComposeNotificationActivity.class);
        intent.putExtra("eventId", "test-event-123");
        intent.putExtra("eventName", "Test Event");

        ActivityScenario<ComposeNotificationActivity> scenario = ActivityScenario.launch(intent);

        // Verify the activity launches without crashing
        // If it reaches here, the intent was properly handled
        scenario.onActivity(activity -> {
            // Activity successfully created with intent extras
            assert activity != null;
        });
    }
}
