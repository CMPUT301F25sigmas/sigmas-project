package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;

// For Espresso view matchers and actions
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;

// For root matchers (Toast detection)
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.atlasevents.data.FakeUserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import android.widget.EditText;
import android.view.View;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

@RunWith(AndroidJUnit4.class)
public class SignUpActivityIntentTest {

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

    @Test
    public void testCancelButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent)) {

            // Click the cancel button
            onView(withId(R.id.cancelButton)).perform(click());

            // Check that the activity is finishing
            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing());
            });

        }
    }

    @Test
    public void testEmailRequirementNoAtSign(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);
        // Fill in form with no @
        onView(withId(R.id.name)).perform(typeText("TestUser"));
        onView(withId(R.id.email)).perform(typeText("testuserexample.com"));
        onView(withId(R.id.phone)).perform(typeText("1234567890"));
        onView(withId(R.id.newPassword)).perform(typeText("password"));

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait a moment for the error to be set
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the name field has an error (any error)
        onView(withId(R.id.email)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            assert view instanceof EditText : "Expected EditText";
            EditText editText = (EditText) view;
            assert editText.getError() != null : "Expected an error message but got null";
        });
    }
    @Test
    public void testNoPhone(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with short name
        onView(withId(R.id.name)).perform(typeText("TestUser"));
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"));
        onView(withId(R.id.newPassword)).perform(typeText("password"));

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait a moment for the error to be set
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the name field has an error (any error)
        onView(withId(R.id.phone)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            assert view instanceof EditText : "Expected EditText";
            EditText editText = (EditText) view;
            assert editText.getError() != null : "Expected an error message but got null";
        });
    }

    @Test
    public void testNoName(){
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with no name
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"));
        onView(withId(R.id.phone)).perform(typeText("1234567890"));
        onView(withId(R.id.newPassword)).perform(typeText("password"));

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait a moment for the error to be set
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the name field has an error (any error)
        onView(withId(R.id.name)).check((view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            assert view instanceof EditText : "Expected EditText";
            EditText editText = (EditText) view;
            assert editText.getError() != null : "Expected an error message but got null";
        });
    }
    @Test
    public void testPasswordRequirementsTooShort() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Fill in form with invalid password
        onView(withId(R.id.name)).perform(typeText("TestUser"));
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"));
        onView(withId(R.id.phone)).perform(typeText("1234567890"));
        onView(withId(R.id.newPassword)).perform(typeText("pwd"));
        
        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Check that the password field shows the error message
        onView(withId(R.id.newPassword))
                .check(matches(hasErrorText("Password must be at least 8 characters")));
    }
    @Test
    public void testSignUpCreatesUserAndNavigatesHome() throws InterruptedException {
        // Launch SignUpActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Use CountDownLatch to ensure injection completes synchronously
        CountDownLatch injectionLatch = new CountDownLatch(1);
        FakeUserRepository fakeRepo = new FakeUserRepository();

        // Inject FakeUserRepository using reflection
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

        // Wait for injection to complete before proceeding
        if (!injectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Repository injection timed out!");
        }

        // Fill in all required fields
        onView(withId(R.id.name)).perform(typeText("TestUser"));
        onView(withId(R.id.email)).perform(typeText("testuser@example.com"));
        onView(withId(R.id.phone)).perform(typeText("1234567890"));
        onView(withId(R.id.newPassword)).perform(typeText("password123"));

        // Click sign up button
        onView(withId(R.id.createButton)).perform(click());

        // Wait for async operation to complete
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }
}
