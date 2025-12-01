package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;

// For Espresso view matchers and actions
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;

// For root matchers (Toast detection)
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import android.widget.EditText;
import android.view.View;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import androidx.test.espresso.Root;
import android.view.WindowManager;

/**
 * Intent Tests for the signInActivity
 **/
@RunWith(AndroidJUnit4.class)
public class SignInActivityIntentTest {

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
     * test that the joinNow button brings you to sign up page
     */
    @Test
    public void testJoinNowButton(){
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.joinNow)).perform(click());

        intended(hasComponent(SignUpActivity.class.getName()));



    }

    /**
     * Test that signing in with proper email and password works and opens entrant dash
     */
    @Test
    public void testSignInAndNavigatesHome() throws InterruptedException {
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        // Use CountDownLatch to ensure injection completes synchronously
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

        // Inject FakeUserRepository
        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignInActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeRepo);
                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Fill in fields
        onView(withId(R.id.emailOrPhone)).perform(typeText("entrant@test.com"));
        onView(withId(R.id.password)).perform(typeText("password"));

        // Click sign in button
        onView(withId(R.id.signInButton)).perform(click());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }



    /**
     * test that using the wrong password does not change pages
     */
    @Test
    public void testWrongPassword(){
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

        // Inject FakeUserRepository
        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignInActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeRepo);
                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Fill in fields
        onView(withId(R.id.emailOrPhone)).perform(typeText("entrant@test.com"));
        onView(withId(R.id.password)).perform(typeText("wrongpassword"));

        // Click sign in button
        onView(withId(R.id.signInButton)).perform(click());

        // Wait for async operation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that we're still on SignInActivity (didn't navigate away)
        onView(withId(R.id.signInButton)).check(matches(isDisplayed()));
    }
    /**
     * test that using the wrong email does not change pages
     */
    @Test
    public void testWrongUsername() throws InterruptedException {
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        // Inject FakeUserRepository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();
        
        final View[] decorView = new View[1];
        
        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignInActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, fakeRepo);
                decorView[0] = activity.getWindow().getDecorView();
                injectionLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Use a non-existent email
        onView(withId(R.id.emailOrPhone)).perform(typeText("nonexistent@test.com"));
        onView(withId(R.id.password)).perform(typeText("password"));

        // Click sign in button
        onView(withId(R.id.signInButton)).perform(click());

        // Wait for toast to appear
        Thread.sleep(800);

        // Verify that we're still on SignInActivity (didn't navigate away)
        onView(withId(R.id.signInButton)).check(matches(isDisplayed()));

    }

}

