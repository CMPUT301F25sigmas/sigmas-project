package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OrganizerNotificationsCenterIntentTest {
    private static final String EMAIL = "intent@tests.com";
    private ActivityScenario<NotificationCenterActivity> scenario;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Session session = new Session(context);
        session.setUserEmail(EMAIL);

        Intents.init();
        scenario = ActivityScenario.launch(NotificationCenterActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        Intents.release();

        Context context = ApplicationProvider.getApplicationContext();
        Session session = new Session(context);
        session.logout();
    }

    @Test
    public void testEventCardClick() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.events_container)).perform(click());
        intended(hasComponent(ComposeNotificationActivity.class.getName()));
    }

    @Test
    public void testHistoryClick() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.notificationHistory)).perform(click());
        intended(hasComponent(NotificationHistoryActivity.class.getName()));
    }
}
