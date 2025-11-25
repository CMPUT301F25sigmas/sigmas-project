package com.example.atlasevents;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.atlasevents.utils.NotificationHelper;

import java.util.List;

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
    private ListenerRegistration badgeListener;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private ActivityResultLauncher<String> requestPermissionLauncher;

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

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue with notification updates.
                startNotificationBadgeListener();
            } else {
                // Permission is denied. Only affects launcher badge updates.
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted, start listener directly
                startNotificationBadgeListener();
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // For older Android versions, permission is granted at install time, so start listener directly
            startNotificationBadgeListener();
        }
    }

    @Override
    protected void onStop() {
        stopNotificationBadgeListener();
        super.onStop();
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
        findViewById(R.id.view_switcher).setOnClickListener(v -> switchToOrganizerView());
        findViewById(R.id.profile_icon).setOnClickListener(v -> openProfile());
        findViewById(R.id.my_events_icon).setOnClickListener(v -> openMyEvents());
        findViewById(R.id.search_icon).setOnClickListener(v -> openSearch());
        findViewById(R.id.notifications_icon).setOnClickListener(v -> openNotifications());
        findViewById(R.id.qr_reader_icon).setOnClickListener(v -> openQrReader());
        findViewById(R.id.logout_icon).setOnClickListener(v -> session.logoutAndRedirect(this));
    }

    /**
     * Sets the active state for the current screen's navigation icon.
     */
    protected void setActiveNavItem(int iconCardId) {
        ((MaterialCardView) findViewById(R.id.profile_icon)).setCardBackgroundColor(Color.parseColor("#676767"));
        ((MaterialCardView) findViewById(R.id.events_icon_card)).setCardBackgroundColor(Color.TRANSPARENT);
        ((MaterialCardView) findViewById(R.id.search_icon_card)).setCardBackgroundColor(Color.TRANSPARENT);

        ((MaterialCardView) findViewById(iconCardId)).setCardBackgroundColor(Color.parseColor("#E8DEF8"));
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
    }
    protected void openQrReader() {
        //Need to do
    }

    private void startNotificationBadgeListener() {
        String email = session.getUserEmail();
        if (email == null) {
            return;
        }
        stopNotificationBadgeListener();
        firestore.collection("users")
                .document(email)
                .collection("preferences")
                .document("blockedOrganizers")
                .get()
                .addOnSuccessListener(prefSnapshot -> {
                    java.util.List<String> blocked = new java.util.ArrayList<>();
                    if (prefSnapshot.exists()) {
                        java.util.List<String> stored = (java.util.List<String>) prefSnapshot.get("blockedEmails");
                        if (stored != null) {
                            blocked.addAll(stored);
                        }
                    }
                    badgeListener = firestore.collection("users")
                            .document(email)
                            .collection("notifications")
                            .addSnapshotListener((snapshot, error) -> {
                                if (error != null || snapshot == null) {
                                    updateBadge(0);
                                    return;
                                }
                                int unread = 0;
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    Boolean read = doc.getBoolean("read");
                                    String organizer = doc.getString("fromOrganizeremail");
                                    if (organizer != null && blocked.contains(organizer)) {
                                        continue;
                                    }
                                    if (read == null || !read) {
                                        unread++;
                                    }
                                }
                                updateBadge(unread);
                            });
                })
                .addOnFailureListener(e -> updateBadge(0));
    }

    private void stopNotificationBadgeListener() {
        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }
    }

    private void updateBadge(int count) {
        android.widget.TextView badge = findViewById(R.id.notifications_badge);
        if (badge == null) {
            NotificationHelper.updateAppBadge(this, count);
            return;
        }
        if (count > 0) {
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
            badge.setVisibility(android.view.View.VISIBLE);
        } else {
            badge.setVisibility(android.view.View.GONE);
        }
        NotificationHelper.updateAppBadge(this, count);
    }

    /**
     * Switches to the organizer view for the current user.
     */
    protected void switchToOrganizerView() {
        Intent intent = new Intent(this, OrganizerDashboardActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
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