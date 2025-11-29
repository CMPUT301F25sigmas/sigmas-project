package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.atlasevents.data.FakeUserRepository;
import com.example.atlasevents.data.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

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

    @Test
    public void testSignUpCreatesUserAndNavigatesHome() {
        // Launch SignUpActivity
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SignUpActivity.class);
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(intent);

        // Inject FakeUserRepository using reflection to ensure it's set
        scenario.onActivity(activity -> {
            try {
                Field userRepoField = SignUpActivity.class.getDeclaredField("userRepo");
                userRepoField.setAccessible(true);
                userRepoField.set(activity, new FakeUserRepository());
                
                // Verify it was set correctly
                UserRepository repo = (UserRepository) userRepoField.get(activity);
                android.util.Log.d("Test", "Repository type: " + repo.getClass().getSimpleName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject FakeUserRepository", e);
            }
        });

        // Wait a moment to ensure injection is complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that EntrantDashboardActivity was launched
        intended(hasComponent(EntrantDashboardActivity.class.getName()));
    }
}
