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
 * Abstract base activity for all admin-related screens in the Atlas Events application.
 * <p>
 * This class provides common functionality for admin activities including a shared
 * navigation sidebar, session management, and user repository access. Child activities
 * extend this class to inherit the navigation structure and can override navigation
 * methods to customize behavior.
 * </p>
 * <p>
 * The base layout includes a vertical navigation sidebar with icons for
 * notifications, events, images, organizers, profiles, and logout functionality.
 * </p>
 *
 * @see Session
 * @see UserRepository
 */
public abstract class AdminBase extends AppCompatActivity {

    protected LinearLayout contentContainer;
    protected Session session;

    protected UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_base);

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
     * Sets up click listeners for all navigation sidebar icons based on the new admin XML.
     * <p>
     * Configures navigation to notifications, events, images, organizers, profiles, and logout.
     * Each icon triggers the corresponding navigation method when clicked.
     * </p>
     */
    private void SidebarNavigation() {
        findViewById(R.id.notifications_icon).setOnClickListener(v -> openNotifications());
        findViewById(R.id.events_icon).setOnClickListener(v -> openEvents());
        findViewById(R.id.images_icon).setOnClickListener(v -> openImages());
        findViewById(R.id.organizers_icon).setOnClickListener(v -> openOrganizers());
        findViewById(R.id.profiles_icon).setOnClickListener(v -> openProfiles());
        findViewById(R.id.logout_icon).setOnClickListener(v -> session.logoutAndRedirect(this));
    }

    /** Opens the notifications screen. */
    protected void openNotifications() {
        Intent intent = new Intent(this, NotificationHistoryActivity.class);
        startActivity(intent);
    }

    /** Opens the events management screen. */
    protected void openEvents() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /** Opens the images management screen. */
    protected void openImages() {}

    /** Opens the organizers management screen. */
    protected void openOrganizers() {}

    /** Opens the profiles management screen. */
    protected void openProfiles() {}

    /**
     * Inflates and adds a layout to the content container.
     * <p>
     * Child activities use this method to set their specific content layout
     * within the base navigation structure. The layout is inflated into the
     * {@link #contentContainer}.
     * </p>
     *
     * @param layoutResId The resource ID of the layout to inflate
     */
    protected void setContentLayout(int layoutResId) {
        getLayoutInflater().inflate(layoutResId, contentContainer, true);
    }
}