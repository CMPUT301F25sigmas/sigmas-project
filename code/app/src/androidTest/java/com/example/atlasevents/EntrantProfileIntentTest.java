package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EntrantProfileIntentTest {

    private static final String TEST_ENTRANT_EMAIL = "entrant@test.com";
    private ActivityScenario<EntrantProfileActivity> scenario;

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

        // Close scenario if open
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
    }

    @Test
    public void testLaunch() throws InterruptedException {
        // Launch EntrantProfileActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantProfileActivity.class);
        ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeUserRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeUserRepository into EntrantBase's userRepository field
                Field userRepoField = EntrantBase.class.getDeclaredField("userRepository");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeUserRepo);

                // Trigger reload after injection by calling the private method
                java.lang.reflect.Method loadMethod = EntrantProfileActivity.class.getDeclaredMethod("loadUserDetails");
                loadMethod.setAccessible(true);
                loadMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        Thread.sleep(1000);

        // Verify the activity launched
        intended(hasComponent(EntrantProfileActivity.class.getName()));
    }

    @Test
    public void testProfileFieldsDisplayed() throws InterruptedException {
        // Launch EntrantProfileActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantProfileActivity.class);
        ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeUserRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeUserRepository into EntrantBase's userRepository field
                Field userRepoField = EntrantBase.class.getDeclaredField("userRepository");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeUserRepo);

                // Trigger reload after injection
                java.lang.reflect.Method loadMethod = EntrantProfileActivity.class.getDeclaredMethod("loadUserDetails");
                loadMethod.setAccessible(true);
                loadMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        Thread.sleep(1000);

        // Verify profile fields are displayed
        onView(withId(R.id.name)).check(matches(isDisplayed()));
        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.phone)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
    }

    @Test
    public void testUserDetailsLoaded() throws InterruptedException {
        // Launch EntrantProfileActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantProfileActivity.class);
        scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            Log.d("TEST", "Activity created: " + activity);
            Log.d("TEST", "Session email: " + new Session(activity).getUserEmail()); });
        // Inject NEW fake repository for this test
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeUserRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeUserRepository into EntrantBase's userRepository field
                Field userRepoField = EntrantBase.class.getDeclaredField("userRepository");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeUserRepo);

                // Trigger reload after injection
                java.lang.reflect.Method loadMethod = EntrantProfileActivity.class.getDeclaredMethod("loadUserDetails");
                loadMethod.setAccessible(true);
                loadMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Wait a bit longer for data to load
        Thread.sleep(2000);

        // Debug: First check what's actually displayed
        scenario.onActivity(activity -> {
            EditText nameField = activity.findViewById(R.id.name);
            EditText emailField = activity.findViewById(R.id.email);
            Log.d("TEST_DEBUG", "Name field text: " + nameField.getText());
            Log.d("TEST_DEBUG", "Email field text: " + emailField.getText());
        });

        Thread.sleep(500);

        // Verify the test user's information is displayed
        onView(withId(R.id.name)).check(matches(withText("Test Entrant")));
        onView(withId(R.id.email)).check(matches(withText("entrant@test.com")));
    }

    @Test
    public void testButtonsDisplayed() throws InterruptedException {
        // Launch EntrantProfileActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantProfileActivity.class);
        ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeUserRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeUserRepository into EntrantBase's userRepository field
                Field userRepoField = EntrantBase.class.getDeclaredField("userRepository");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeUserRepo);

                // Trigger reload after injection
                java.lang.reflect.Method loadMethod = EntrantProfileActivity.class.getDeclaredMethod("loadUserDetails");
                loadMethod.setAccessible(true);
                loadMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        Thread.sleep(1000);

        // Verify buttons are displayed
        onView(withId(R.id.createButton)).check(matches(isDisplayed())); // Save button
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()));
        onView(withId(R.id.deleteProfileButton)).check(matches(isDisplayed()));
    }
    @Test
    public void testUserDetailsChangesCancelled() throws InterruptedException {
        // Launch EntrantProfileActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantProfileActivity.class);
        ActivityScenario<EntrantProfileActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeUserRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                // Inject FakeUserRepository into EntrantBase's userRepository field
                Field userRepoField = EntrantBase.class.getDeclaredField("userRepository");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeUserRepo);

                // Trigger reload after injection
                java.lang.reflect.Method loadMethod = EntrantProfileActivity.class.getDeclaredMethod("loadUserDetails");
                loadMethod.setAccessible(true);
                loadMethod.invoke(activity);

                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait for injection to complete
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        Thread.sleep(1000);

        // FIRST: Clear the text field completely
        onView(withId(R.id.name)).perform(clearText());
        Thread.sleep(500);

        // SECOND: Type the new text and close keyboard to avoid extra characters
        onView(withId(R.id.name)).perform(typeText("New Name"), closeSoftKeyboard());
        Thread.sleep(500);

        // Verify new text (without the extra 'h')
        onView(withId(R.id.name)).check(matches(withText("New Name")));

        Thread.sleep(500);

        // THIRD: Click cancel button
        onView(withId(R.id.cancelButton)).perform(click());

        Thread.sleep(1000); // Give more time for cancellation to process

        // FOURTH: Close keyboard again and check original text
        onView(isRoot()).perform(closeSoftKeyboard());
        Thread.sleep(500);

        // Verify original text is restored
        onView(withId(R.id.name)).check(matches(withText("Test Entrant")));
    }

}
