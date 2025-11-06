package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;

/**
 * Base activity for all organizer-related screens in the application.
 * <p>
 * This abstract class provides common functionality for organizer activities including
 * a consistent sidebar navigation, session management, and user repository access.
 * Child activities inherit the base layout and navigation while providing their own
 * content through the content container.
 * </p>
 * <p>
 * The base layout includes a sidebar with navigation icons for:
 * </p>
 * <ul>
 *   <li>Settings</li>
 *   <li>Profile</li>
 *   <li>My Events</li>
 *   <li>Create Events</li>
 *   <li>Notifications</li>
 *   <li>Logout</li>
 * </ul>
 *
 * @see AppCompatActivity
 * @see Session
 * @see UserRepository
 */
public abstract class OrganizerBase extends AppCompatActivity {

    /**
     * Container where child activity layouts are inflated.
     * This allows each child activity to define its own content while maintaining
     * the common base layout and navigation.
     */
    protected LinearLayout contentContainer;

    /**
     * Session manager for handling user authentication state and logout operations.
     */
    protected Session session;

    /**
     * Repository for accessing and managing user data.
     */
    protected UserRepository userRepository;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the base layout, sets up window insets for edge-to-edge display,
     * initializes the content container, session manager, and user repository,
     * and configures the sidebar navigation.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_base);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contentContainer = findViewById(R.id.content_container);
        session = new Session(this);
        userRepository = new UserRepository();

        SidebarNavigation();
    }

    /**
     * Sets up click listeners for all sidebar navigation icons.
     * <p>
     * Configures navigation to different organizer screens and handles logout functionality.
     * Each icon is mapped to its corresponding navigation method.
     * </p>
     */
    private void SidebarNavigation() {
        findViewById(R.id.settings_icon).setOnClickListener(v -> openSettings());
        findViewById(R.id.profile_icon).setOnClickListener(v -> openProfile());
        findViewById(R.id.my_events_icon).setOnClickListener(v -> openMyEvents());
        findViewById(R.id.create_events_icon).setOnClickListener(v -> openCreateEvents());
        findViewById(R.id.notifications_icon).setOnClickListener(v -> openNotifications());
        findViewById(R.id.logout_icon).setOnClickListener(v -> session.logoutAndRedirect(this));
    }


    protected void openSettings() {}

    /**
     * Opens the organizer profile screen.
     * <p>
     * Navigates to {@link OrganizerProfileActivity}, finishes the current activity,
     * and disables transition animations for a seamless navigation experience.
     * </p>
     */
    protected void openProfile() {
        Intent intent = new Intent(this, OrganizerProfileActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Opens the organizer dashboard showing all events.
     * <p>
     * Navigates to {@link OrganizerDashboardActivity}, finishes the current activity,
     * and disables transition animations.
     * </p>
     */
    protected void openMyEvents() {
        Intent intent = new Intent(this, OrganizerDashboardActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Opens the event creation screen.
     * <p>
     * Navigates to {@link CreateEventActivity}. Does not finish the current activity,
     * allowing the user to return to the previous screen.
     * </p>
     */
    protected void openCreateEvents() {
        Intent intent = new Intent(this, CreateEventActivity.class);
        startActivity(intent);
    }


    protected void openNotifications() {}

    /**
     * Inflates a layout resource into the content container.
     * <p>
     * This method allows child activities to define their own content layouts
     * while maintaining the common base layout and navigation. The specified
     * layout is inflated and added to the content container.
     * </p>
     *
     * @param layoutResId The resource ID of the layout to inflate
     */
    protected void setContentLayout(int layoutResId) {
        getLayoutInflater().inflate(layoutResId, contentContainer, true);
    }
}