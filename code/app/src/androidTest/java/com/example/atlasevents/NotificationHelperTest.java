package com.example.atlasevents;


/**
 * Unit tests for the {@link NotificationHelper} class.
 * Tests the UI helper methods for displaying notifications as dialogs and snackbars.
 * Uses mocking to verify UI interactions without requiring Android runtime.
 *
 * @see NotificationHelper
 * @see AlertDialog
 * @see Snackbar
 */
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.atlasevents.OrganizerDashboardActivity;
import com.example.atlasevents.utils.NotificationHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the {@link NotificationHelper} class.
 * These tests run on a real Android device or emulator.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationHelperTest {

    // Rule to launch a real activity before each test.
    @Rule
    public ActivityScenarioRule<OrganizerDashboardActivity> activityRule = new ActivityScenarioRule<>(OrganizerDashboardActivity.class);

    /**
     * Tests that showInAppSnackbar correctly displays a Snackbar on the screen.
     */
    @Test
    public void testShowInAppSnackbar_DisplaysSnackbar() {
        // Arrange
        String testMessage = "Snackbar Message";

        // Act
        activityRule.getScenario().onActivity(activity -> {
            NotificationHelper.showInAppSnackbar(activity, testMessage);
        });

        // Assert: Use Espresso to check if the snackbar with the correct text is displayed.
        onView(withText(testMessage)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the helper methods do not crash with null activity.
     */
    @Test
    public void testMethods_WithNullActivity_DoNotCrash() {
        // This test is still valid and important.
        // It ensures your null-checks are working.
        NotificationHelper.showInAppDialog(null, "Title", "Message");
        NotificationHelper.showInAppSnackbar(null, "Message");
        assertTrue(true); // Test passes if no exception was thrown
    }
}
