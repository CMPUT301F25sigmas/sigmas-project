package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;

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

    private void SidebarNavigation() {
        findViewById(R.id.settings_icon).setOnClickListener(v -> openSettings());
        findViewById(R.id.profile_icon).setOnClickListener(v -> openProfile());
        findViewById(R.id.my_events_icon).setOnClickListener(v -> openMyEvents());
        findViewById(R.id.search_icon).setOnClickListener(v -> openSearch());
        findViewById(R.id.notifications_icon).setOnClickListener(v -> openNotifications());
        findViewById(R.id.qr_reader_icon).setOnClickListener(v -> openQrReader());
        findViewById(R.id.logout_icon).setOnClickListener(v -> session.logoutAndRedirect(this));
    }

    protected void openSettings() {}
    protected void openProfile() {
        Intent intent = new Intent(this, EntrantProfileActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
    protected void openMyEvents() {
        Intent intent = new Intent(this, EntrantDashboardActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
    protected void openSearch() {
        Intent intent = new Intent(this, EntrantSearchActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
    protected void openNotifications() {}
    protected void openQrReader() {}

    protected void setContentLayout(int layoutResId) {
        getLayoutInflater().inflate(layoutResId, contentContainer, true);
    }
}
