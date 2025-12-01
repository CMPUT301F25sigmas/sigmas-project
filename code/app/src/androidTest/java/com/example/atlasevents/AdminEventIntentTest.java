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
public class AdminEventIntentTest {

    @Rule
    public ActivityScenarioRule<AdminDashboardActivity> activityRule = new ActivityScenarioRule<>(AdminDashboardActivity.class);

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
     * Test: Verify the dropdown buttons and that the view details option is functional
     */
    @Test
    public void testDropdown() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {}

            @Override
            public boolean matchesSafely(View view) {
                return view.getId() == R.id.menu_button && currentIndex++ == 0;
            }
        }).perform(click());

        // Wait for dropdown to appear
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.action_remove_event)).check(matches(isDisplayed()));
        onView(withId(R.id.action_view_details)).check(matches(isDisplayed()));

        onView(withId(R.id.action_view_details)).perform(click());

        intended(hasComponent(EventDetailsAdminActivity.class.getName()));
        onView(withId(R.id.back_arrow)).check(matches(isDisplayed()));
    }
}
