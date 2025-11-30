package com.example.atlasevents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OrganizerMyEventIntentTest {
    private static final String EMAIL = "intent@tests.com";
    private ActivityScenario<OrganizerDashboardActivity> scenario;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Session session = new Session(context);
        session.setUserEmail(EMAIL);

        Intents.init();
        scenario = ActivityScenario.launch(OrganizerDashboardActivity.class);
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

        onView(withId(R.id.events_container_organizer)).perform(click());
        intended(hasComponent(EventManageActivity.class.getName()));
    }

    @Test
    public void testEventEdit() {
        // Wait for events to load from Firebase
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {}

            @Override
            public boolean matchesSafely(View view) {
                return view.getId() == R.id.edit_button && currentIndex++ == 0;
            }
        }).perform(click());

        intended(hasComponent(EditEventActivity.class.getName()));
    }
}
