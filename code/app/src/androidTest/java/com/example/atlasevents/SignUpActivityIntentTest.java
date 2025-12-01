package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Intent test for SignUpActivity
 */
@RunWith(AndroidJUnit4.class)
public class SignUpActivityIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * test the cancel button closes the activity
     */
    @Test
    public void testCancelButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.cancelButton)).perform(click());
            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing());
            });
        }
    }

    @Test
    public void testEmailRequirementNoAtSign() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with no @ in email
        onView(withId(R.id.name)).perform(typeText("TestUser"), closeSoftKeyboard());
        onView(withId(R.id.email)).perform(typeText("testuserexample.com"), closeSoftKeyboard());
        onView(withId(R.id.phone)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.newPassword)).perform(typeText("password123"), closeSoftKeyboard());

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for validation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the email field has an error (any error message is fine)
        onView(withId(R.id.email)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            android.widget.EditText editText = (android.widget.EditText) view;
            CharSequence error = editText.getError();
            assert error != null : "Expected an error message but got null";
            // Check that it mentions email being invalid
            assert error.toString().toLowerCase().contains("email") ||
                    error.toString().toLowerCase().contains("invalid") :
                    "Error should mention email or invalid, got: " + error.toString();
        });

        scenario.close();
    }

    @Test
    public void testNoPhone() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Inject fake repository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignUpActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeRepo);
                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Fill in form without phone (phone is optional based on validatePhone with false param)
        onView(withId(R.id.name)).perform(typeText("TestUser"), closeSoftKeyboard());
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"), closeSoftKeyboard());
        onView(withId(R.id.newPassword)).perform(typeText("password123"), closeSoftKeyboard());

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for async operation
        Thread.sleep(1500);

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));

        scenario.close();
    }

    @Test
    public void testNoName() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with no name
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"), closeSoftKeyboard());
        onView(withId(R.id.phone)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.newPassword)).perform(typeText("password123"), closeSoftKeyboard());

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for validation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the name field has an error (check for any error, not specific message)
        onView(withId(R.id.name)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            android.widget.EditText editText = (android.widget.EditText) view;
            assert editText.getError() != null : "Expected an error message but got null";
        });

        scenario.close();
    }

    @Test
    public void testPasswordRequirementsTooShort() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with invalid password
        onView(withId(R.id.name)).perform(typeText("TestUser"), closeSoftKeyboard());
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"), closeSoftKeyboard());
        onView(withId(R.id.phone)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.newPassword)).perform(typeText("pwd"), closeSoftKeyboard());

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for validation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the password field has an error (match whatever InputValidator returns)
        onView(withId(R.id.newPassword)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            android.widget.EditText editText = (android.widget.EditText) view;
            CharSequence error = editText.getError();
            assert error != null : "Expected an error message but got null";
            // Check that it mentions password length requirement
            assert error.toString().toLowerCase().contains("password") : "Error should mention password";
        });

        scenario.close();
    }

    @Test
    public void testSignUpCreatesUserAndNavigatesHome() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Inject FakeUserRepository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignUpActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeRepo);
                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Fill in all required fields
        onView(withId(R.id.name)).perform(typeText("TestUser"), closeSoftKeyboard());
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"), closeSoftKeyboard());
        onView(withId(R.id.phone)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.newPassword)).perform(typeText("password123"), closeSoftKeyboard());

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for async operation
        Thread.sleep(1500);

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));

        scenario.close();
    }
}