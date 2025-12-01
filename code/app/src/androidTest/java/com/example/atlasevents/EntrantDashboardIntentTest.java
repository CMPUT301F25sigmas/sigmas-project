package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.ViewAssertion;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Intent Tests for EntrantDashboardActivity
 **/
@RunWith(AndroidJUnit4.class)
public class EntrantDashboardIntentTest {

    private static final String TEST_ENTRANT_EMAIL = "entrant@test.com";

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    );

    @Before
    public void setUp() {
        // Initialize Intents before each test
        Intents.init();

        // Set up session with test entrant email
        Context context = ApplicationProvider.getApplicationContext();
        Session session = new Session(context);
        session.setUserEmail(TEST_ENTRANT_EMAIL);
    }

    @After
    public void tearDown() {
        // Release Intents after each test
        Intents.release();

        // Clean up session
        Context context = ApplicationProvider.getApplicationContext();
        Session session = new Session(context);
        session.logout();
    }

    @Test
    public void testLaunch() {
        // Launch EntrantDashboardActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantDashboardActivity.class);
        ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repositories
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeEventRepository fakeEventRepo = new FakeEventRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeEventRepository
                Field eventRepoField = EntrantDashboardActivity.class.getDeclaredField("eventRepository");
                eventRepoField.setAccessible(true);
                eventRepoField.set(activity, fakeEventRepo);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeEventRepository", e);
            }
        });


        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }

    /**
     * Custom ViewAssertion to check that a view does not exist in the hierarchy
     */
    public static ViewAssertion doesNotExist() {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                // View is not present
                return;
            }
            throw new AssertionError("View is present in the hierarchy");
        };
    }


}
