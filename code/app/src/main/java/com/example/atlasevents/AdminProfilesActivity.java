package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

import java.util.ArrayList;

/**
 * Activity displaying the admin's dashboard with a list of available events.
 * <p>
 * This activity extends {@link AdminBase} to provide the navigation sidebar and
 * displays all events retrieved from Firebase. Events are shown as cards that admins
 * can tap to view detailed information. The activity handles fetching events from
 * the repository and dynamically creating event card views.
 * </p>
 *
 * @see AdminBase
 * @see Event
 * @see EventRepository
 * @see EventDetailsActivity
 */
public class AdminProfilesActivity extends AdminBase {

    /**
     * Container layout that holds all event card views.
     */
    private LinearLayout profilesContainer;

    /**
     * Repository for fetching event data from Firebase.
     */
    private UserRepository userRepository;

    /**
     * Scroll view containing the list of events.
     */
    private ScrollView profilesScrollView;

    /**
     * Layout for displaying a message when no events are available.
     */
    private LinearLayout emptyState;

    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.admin_profiles);

        userType = getIntent().getStringExtra("userType");

        TextView pageTitle = findViewById(R.id.page_title);
        pageTitle.setText(userType.equals("Entrant")? "Entrants" : "Organizers");

        profilesContainer = findViewById(R.id.profiles_container_organizer);
        userRepository = new UserRepository();

        profilesScrollView = findViewById(R.id.profiles_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        profilesScrollView.setVisibility(View.GONE);

        loadProfilesFromFirebase();
    }

    /**
     * Fetches all events from Firebase and displays them.
     * <p>
     * Makes an asynchronous call to the event repository to retrieve all available
     * events. On success, the events are passed to {@link #displayEvents(ArrayList)}
     * for rendering. On failure, {@link #showEmptyState()} is called to show an
     * empty state layout.
     * </p>
     */
    private void loadProfilesFromFirebase() {
        userRepository.getUsers(userType, new UserRepository.UsersCallback(){
            @Override
            public void onSuccess(ArrayList<User> users) {
                if (users.isEmpty()) {
                    showEmptyState();
                } else {
                    displayEvents(users);
                }
            }

            @Override
            public void onFailure(Exception e) {
                showEmptyState();
            }
        });
    }

    /**
     * Displays a list of events as card views in the events container.
     * <p>
     * Clears any existing event cards and dynamically inflates new card views
     * for each event in the list. Each card shows the event name and image,
     * and is clickable to open event details.
     * </p>
     *
     * @param events The list of events to display
     */
    private void displayEvents(ArrayList<User> users) {
        emptyState.setVisibility(View.GONE);
        profilesScrollView.setVisibility(View.VISIBLE);
        profilesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (User user : users) {
            View userCard = inflater.inflate(R.layout.profile_card_admin_item, profilesContainer, false);

            ImageView menuButton = userCard.findViewById(R.id.menu_button);
            TextView userName = userCard.findViewById(R.id.user_name);

            userName.setText(user.getName());

            menuButton.setOnClickListener(v -> {
                View dropdownView = inflater.inflate(R.layout.profile_dropdown, null);

                PopupWindow popupWindow = new PopupWindow(dropdownView, userCard.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);

                popupWindow.setOutsideTouchable(true);

                popupWindow.showAsDropDown(userCard, 0, -userCard.getHeight()+150);

                dropdownView.findViewById(R.id.action_view_details).setOnClickListener(item -> {
                    openUserDetails(user);
                    popupWindow.dismiss();
                });

                dropdownView.findViewById(R.id.action_remove_user).setOnClickListener(item -> {
                    userRepository.deleteUser(user.getEmail());
                    loadProfilesFromFirebase();
                    popupWindow.dismiss();
                });
            });

            profilesContainer.addView(userCard);
        }
    }

    private void openUserDetails(User user) {
        Intent intent = new Intent(this, UserDetailsAdminActivity.class);
        intent.putExtra(UserDetailsAdminActivity.UserKey, user.getEmail());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilesFromFirebase();
    }

    /**
     * Shows the empty state layout with a message and hides the events scroll view.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        profilesScrollView.setVisibility(View.GONE);
    }
}