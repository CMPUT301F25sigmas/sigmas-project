package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventIntentTest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

    @Test
    public void testCreateEventUIElementsDisplayed() {
        // Check if all UI elements are displayed
        onView(withId(R.id.nameEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.dateEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.timeEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.regDateEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.descrEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.locEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.publishEventButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateEventWithValidInput() {
        // Enter event details
        onView(withId(R.id.nameEditText))
                .perform(typeText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.descrEditText))
                .perform(typeText("Test Description"), closeSoftKeyboard());
        onView(withId(R.id.locEditText))
                .perform(typeText("Test Location"), closeSoftKeyboard());

        // Click the publish button
        onView(withId(R.id.publishEventButton)).perform(click());

        // Add assertions to verify the expected behavior
        // For example, check if the activity finishes or shows a success message
    }

    @Test
    public void testBackButtonFinishesActivity() {
        // Click the back button
        Espresso.pressBack();

        // Verify the activity is finished
        // Note: This is a simplified check - in a real test, you might need to verify the activity stack
        onView(withId(R.id.main)).check(doesNotExist());
    }

    public static ViewAssertion doesNotExist() {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                // View is not present in the hierarchy
                return;
            }
            throw new AssertionError("View is present in the hierarchy");
        };
    }
}

