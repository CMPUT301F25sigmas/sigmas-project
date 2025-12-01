package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

/**
 * Intent Tests for the signInActivity
 **/
@RunWith(AndroidJUnit4.class)
public class SignInActivityIntentTest {

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        if (prefsDir.exists() && prefsDir.isDirectory()) {
            File[] files = prefsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }


    /**
     * test that the joinNow button brings you to sign up page
     */
    @Test
    public void testJoinNowButton(){
        ActivityScenario.launch(SignInActivity.class);

        onView(withId(R.id.joinNow)).perform(click());
        intended(hasComponent(SignUpActivity.class.getName()));
    }

    /**
     * Test that signing in with proper email and password works and opens entrant dash
     */
    @Test
    public void testSignInAndNavigatesHome() throws InterruptedException {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

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

        injectionLatch.await();

        onView(withId(R.id.emailOrPhone)).perform(typeText("entrant@test.com"));
        onView(withId(R.id.password)).perform(typeText("password"));

        onView(withId(R.id.signInButton)).perform(click());

        Thread.sleep(1500);

        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }



    /**
     * test that using the wrong password does not change pages
     */
    @Test
    public void testWrongPassword() throws InterruptedException {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

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

        injectionLatch.await();

        onView(withId(R.id.emailOrPhone)).perform(typeText("entrant@test.com"));
        onView(withId(R.id.password)).perform(typeText("wrongpassword"));

        onView(withId(R.id.signInButton)).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).check(matches(isDisplayed()));
    }

    /**
     * test that using the wrong email does not change pages
     */
    @Test
    public void testWrongUsername() throws InterruptedException {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

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

        injectionLatch.await();

        onView(withId(R.id.emailOrPhone)).perform(typeText("nonexistent@test.com"));
        onView(withId(R.id.password)).perform(typeText("password"));

        onView(withId(R.id.signInButton)).perform(click());

        Thread.sleep(800);

        onView(withId(R.id.signInButton)).check(matches(isDisplayed()));
    }
}