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

import com.example.atlasevents.EntrantDashboardActivity;
import com.example.atlasevents.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EntrantSearchIntentTest {

    private static final String TEST_ENTRANT_EMAIL = "entrant@test.com";

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
        ActivityScenario<EntrantSearchActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repositories
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeEventRepository fakeEventRepo = new FakeEventRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeEventRepository
                Field eventRepoField = EntrantSearchActivity.class.getDeclaredField("eventRepository");
                eventRepoField.setAccessible(true);
                eventRepoField.set(activity, fakeEventRepo);

                // Trigger reload after injection by calling the private method
                Method fetchMethod = EntrantSearchActivity.class.getDeclaredMethod("fetchOpenEvents");
                fetchMethod.setAccessible(true);
                fetchMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeEventRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        Thread.sleep(1000);

        intended(hasComponent(EntrantSearchActivity.class.getName()));

    }
    @Test
    public void testSearchWithTags() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario<EntrantSearchActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repositories
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeEventRepository fakeEventRepo = new FakeEventRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeEventRepository BEFORE any search happens
                Field eventRepoField = EntrantSearchActivity.class.getDeclaredField("eventRepository");
                eventRepoField.setAccessible(true);
                eventRepoField.set(activity, fakeEventRepo);

                // Clear any pending search handlers to ensure fresh state
                Field searchHandlerField = EntrantSearchActivity.class.getDeclaredField("searchHandler");
                searchHandlerField.setAccessible(true);
                android.os.Handler handler = (android.os.Handler) searchHandlerField.get(activity);
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }

                // Reset search state
                Field lastQueryField = EntrantSearchActivity.class.getDeclaredField("lastRequestedQuery");
                lastQueryField.setAccessible(true);
                lastQueryField.set(activity, "");

                Field lastLengthField = EntrantSearchActivity.class.getDeclaredField("lastRequestedLength");
                lastLengthField.setAccessible(true);
                lastLengthField.set(activity, 0);

                // Trigger reload after injection by calling the private method
                Method fetchMethod = EntrantSearchActivity.class.getDeclaredMethod("fetchOpenEvents");
                fetchMethod.setAccessible(true);
                fetchMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeEventRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Wait for initial events to load
        Thread.sleep(1000);
        
        // Perform search
        onView(withId(R.id.search_view)).perform(click());
        Thread.sleep(500);

        onView(isFocused()).perform(typeText("sports"));
        // Wait for search to complete)
        Thread.sleep(1500);

        // Verify the event is found
        onView(withText("Test Event With Entrant")).check(matches(isDisplayed()));

    }
    @Test
    public void testSearchWithWrongTags() throws InterruptedException {
        // Launch EntrantSearchActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantSearchActivity.class);
        ActivityScenario<EntrantSearchActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repositories
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeEventRepository fakeEventRepo = new FakeEventRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeEventRepository BEFORE any search happens
                Field eventRepoField = EntrantSearchActivity.class.getDeclaredField("eventRepository");
                eventRepoField.setAccessible(true);
                eventRepoField.set(activity, fakeEventRepo);

                // Clear any pending search handlers to ensure fresh state
                Field searchHandlerField = EntrantSearchActivity.class.getDeclaredField("searchHandler");
                searchHandlerField.setAccessible(true);
                android.os.Handler handler = (android.os.Handler) searchHandlerField.get(activity);
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }

                // Reset search state
                Field lastQueryField = EntrantSearchActivity.class.getDeclaredField("lastRequestedQuery");
                lastQueryField.setAccessible(true);
                lastQueryField.set(activity, "");

                Field lastLengthField = EntrantSearchActivity.class.getDeclaredField("lastRequestedLength");
                lastLengthField.setAccessible(true);
                lastLengthField.set(activity, 0);

                // Trigger reload after injection by calling the private method
                Method fetchMethod = EntrantSearchActivity.class.getDeclaredMethod("fetchOpenEvents");
                fetchMethod.setAccessible(true);
                fetchMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeEventRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Wait for initial events to load
        Thread.sleep(1000);

        // Perform search
        onView(withId(R.id.search_view)).perform(click());
        Thread.sleep(500);

        onView(isFocused()).perform(typeText("dance"));
        // Wait for search to complete)
        Thread.sleep(1500);

        // Verify the event is not found
        onView(withText("Test Event With Entrant")).check(EntrantDashboardIntentTest.doesNotExist());

    }

}

