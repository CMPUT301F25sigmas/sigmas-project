package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OrganizerSidebarIntentTest {

    @Rule
    public ActivityScenarioRule<OrganizerDashboardActivity> activityRule = new ActivityScenarioRule<>(OrganizerDashboardActivity.class);

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
     * Test: Verify functioning of notifications sidebar button
     */
    @Test
    public void testSidebarNotifications() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.notifications_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.notifications_icon)).perform(click());

        intended(hasComponent(NotificationCenterActivity.class.getName()));
    }

    /**
     * Test: Verify functioning of profile sidebar button
     */
    @Test
    public void testSidebarProfile() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.profile_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.profile_icon)).perform(click());

        intended(hasComponent(OrganizerProfileActivity.class.getName()));
    }

    /**
     * Test: Verify functioning of my events sidebar button
     */
    @Test
    public void testSidebarMyEvents() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.my_events_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.my_events_icon)).perform(click());

        intended(hasComponent(OrganizerDashboardActivity.class.getName()));
    }

    /**
     * Test: Verify functioning of create events sidebar button
     */
    @Test
    public void testSidebarCreateEvents() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.create_events_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.create_events_icon)).perform(click());

        intended(hasComponent(CreateEventActivity.class.getName()));
    }

    /**
     * Test: Verify functioning of view switcher sidebar button
     */
    @Test
    public void testSidebarViewSwitcher() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.view_switcher)).check(matches(isDisplayed()));

        onView(withId(R.id.view_switcher)).perform(click());

        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }

    /**
     * Test: Verify functioning of logout sidebar button
     */
    @Test
    public void testSidebarLogout() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.logout_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.logout_icon)).perform(click());

        intended(hasComponent(SignInActivity.class.getName()));
    }
}