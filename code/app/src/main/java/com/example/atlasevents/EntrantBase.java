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
 * Abstract base activity for all entrant-related screens in the Atlas Events application.
 * <p>
 * This class provides common functionality for entrant activities including a shared
 * navigation sidebar, session management, and user repository access. Child activities
 * extend this class to inherit the navigation structure and can override navigation
 * methods to customize behavior.
 * </p>
 * <p>
 * The base layout includes a navigation sidebar with icons for settings, profile,
 * events, search, notifications, QR reader, and logout functionality.
 * </p>
 *
 * @see EntrantProfileActivity
 * @see EntrantDashboardActivity
 * @see Session
 * @see UserRepository
 */
public abstract class EntrantBase extends AppCompatActivity {

    protected LinearLayout contentContainer;
    protected Session session;

    protected UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_base);

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
     * Sets up click listeners for all navigation sidebar icons.
     * <p>
     * Configures navigation to settings, profile, events, search, notifications,
     * QR reader, and logout. Each icon triggers the corresponding navigation method
     * when clicked.
     * </p>
     */
    private void SidebarNavigation() {
        findViewById(R.id.settings_icon).setOnClickListener(v -> openSettings());
        findViewById(R.id.profile_icon).setOnClickListener(v -> openProfile());
        findViewById(R.id.my_events_icon).setOnClickListener(v -> openMyEvents());
        findViewById(R.id.search_icon).setOnClickListener(v -> openSearch());
        findViewById(R.id.notifications_icon).setOnClickListener(v -> openNotifications());
        findViewById(R.id.qr_reader_icon).setOnClickListener(v -> openQrReader());
        findViewById(R.id.logout_icon).setOnClickListener(v -> session.logoutAndRedirect(this));
    }

    /**
     * Opens the settings screen.
     */
    protected void openSettings() {}

    /**
     * Opens the entrant profile screen.
     * <p>
     * Navigates to {@link EntrantProfileActivity}, finishes the current activity,
     * and disables transition animations for a seamless navigation experience.
     * </p>
     */
    protected void openProfile() {
        Intent intent = new Intent(this, EntrantProfileActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Opens the entrant events dashboard screen.
     * <p>
     * Navigates to {@link EntrantDashboardActivity}, finishes the current activity,
     * and disables transition animations for a seamless navigation experience.
     * </p>
     */
    protected void openMyEvents() {
        Intent intent = new Intent(this, EntrantDashboardActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Opens the entrant search screen.
     * <p>
     * Navigates to {@link EntrantSearchActivity}, finishes the current activity,
     * and disables transition animations for a seamless navigation experience.
     * </p>
     */
    protected void openSearch() {
        Intent intent = new Intent(this, EntrantSearchActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
    protected void openNotifications() {
        Intent intent = new Intent(this, NotificationHistoryActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
    protected void openQrReader() {
        //Need to do
    }

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
