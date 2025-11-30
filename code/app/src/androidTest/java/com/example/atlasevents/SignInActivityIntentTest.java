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

    // Add a helper matcher to check if EditText has any error
    private static Matcher<View> hasError() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has error");
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof EditText)) {
                    return false;
                }
                EditText editText = (EditText) view;
                return editText.getError() != null;
            }
        };
    }

    // Add this helper method to create a custom toast root matcher
    private static Matcher<Root> isToast() {
        return new TypeSafeMatcher<Root>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is toast");
            }

            @Override
            public boolean matchesSafely(Root root) {
                int type = root.getWindowLayoutParams().get().type;
                if (type == WindowManager.LayoutParams.TYPE_TOAST) {
                    return true;
                }
                // Also check for application overlay type (some Android versions use this)
                if (type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) {
                    return true;
                }
                return false;
            }
        };
    }
    @Test
    public void testJoinNowButton(){
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.joinNow)).perform(click());

        intended(hasComponent(SignUpActivity.class.getName()));



    }
    @Test
    public void testSignInAndNavigatesHome() throws InterruptedException {
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        // Use CountDownLatch to ensure injection completes synchronously
        CountDownLatch injectionLatch = new CountDownLatch(1);
        EntrantWaitlistIntentTest.FakeUserRepository fakeRepo = new EntrantWaitlistIntentTest.FakeUserRepository();

        // Inject FakeUserRepository using reflection
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

        // Fill in all required fields
        onView(withId(R.id.emailOrPhone)).perform(typeText("entrant@test.com"));
        onView(withId(R.id.password)).perform(typeText("password"));

        // Click sign in button
        onView(withId(R.id.signInButton)).perform(click());

        // Wait for async operation to complete
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }
    @Test
    public void testCancelButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        try (ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent)) {

            // Click the cancel button
            onView(withId(R.id.cancelButton)).perform(click());

            // Check that the activity is finishing
            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing());
            });

        }
    }
    @Test
    public void testWrongPassword(){
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        // Use CountDownLatch to ensure injection completes synchronously
        CountDownLatch injectionLatch = new CountDownLatch(1);
        EntrantWaitlistIntentTest.FakeUserRepository fakeRepo = new EntrantWaitlistIntentTest.FakeUserRepository();

        // Inject FakeUserRepository using reflection
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

        // Fill in all required fields
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
    @Test
    public void testWrongUsername() throws InterruptedException {
        // Launch SignInActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignInActivity.class);
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(intent);

        // Inject FakeUserRepository
        CountDownLatch injectionLatch = new CountDownLatch(1);
        EntrantWaitlistIntentTest.FakeUserRepository fakeRepo = new EntrantWaitlistIntentTest.FakeUserRepository();
        
        // Store decor view for alternative approach
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

