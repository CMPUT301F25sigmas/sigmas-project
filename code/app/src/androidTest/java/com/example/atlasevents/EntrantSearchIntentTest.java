package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocused;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.example.atlasevents.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EntrantSearchIntentTest {

    private static final String TEST_ENTRANT_EMAIL = "intent@tests.com";

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.POST_NOTIFICATIONS
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
    public void testLaunch() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario.launch(intent);

        // Wait for activity to load
        Thread.sleep(2000);

        // Verify that the activity launched successfully
        intended(hasComponent(EntrantSearchActivity.class.getName()));
    }

    @Test
    public void testSearchWithTags() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario.launch(intent);

        // Wait for initial events to load from Firestore
        Thread.sleep(2000);

        // Perform search
        onView(withId(R.id.search_view)).perform(click());
        Thread.sleep(500);

        onView(isFocused()).perform(typeText("sports"));

        // Wait for search to complete
        Thread.sleep(2000);

        // Verify the event is found
        onView(withText("Swimming Competition")).check(matches(isDisplayed()));
    }

    @Test
    public void testSearchWithWrongTags() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario.launch(intent);

        // Wait for initial events to load from Firestore
        Thread.sleep(2000);

        // Perform search
        onView(withId(R.id.search_view)).perform(click());
        Thread.sleep(500);

        onView(isFocused()).perform(typeText("dance"));

        // Wait for search to complete
        Thread.sleep(2000);

        // Verify the event is not found
        onView(withText("Intent Tests Event 1")).check(EntrantDashboardIntentTest.doesNotExist());
    }

    @Test
    public void testSearchWithEventName() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario.launch(intent);

        // Wait for initial events to load from Firestore
        Thread.sleep(2000);

        // Perform search
        onView(withId(R.id.search_view)).perform(click());
        Thread.sleep(500);

        onView(isFocused()).perform(typeText("Swimming"));

        // Wait for search to complete
        Thread.sleep(2000);

        // Verify the event is found
        onView(withText("Swimming Competition")).check(matches(isDisplayed()));
    }
}