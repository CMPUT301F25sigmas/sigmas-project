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

import com.example.atlasevents.data.UserRepository;

import java.util.ArrayList;

/**
 * Activity displaying the admin's dashboard with a list of users.
 * <p>
 * This activity extends {@link AdminBase} to provide the navigation sidebar and
 * displays all users retrieved from Firebase based on type. Users are shown as cards
 * that admins can tap to view detailed information. The activity handles fetching
 * users from the repository and dynamically creating user card views.
 * </p>
 *
 * @see AdminBase
 * @see User
 * @see UserRepository
 * @see UserDetailsAdminActivity
 */
public class AdminProfilesActivity extends AdminBase {

    /**
     * Container layout that holds all user card views.
     */
    private LinearLayout usersContainer;

    /**
     * Repository for fetching user data from Firebase.
     */
    private UserRepository userRepository;

    /**
     * Scroll view containing the list of users.
     */
    private ScrollView usersScrollView;

    /**
     * Layout for displaying a message when no users are available.
     */
    private LinearLayout emptyState;

    /**
     * The type of users to display
     */
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.admin_profiles);

        userType = getIntent().getStringExtra("userType");

        TextView pageTitle = findViewById(R.id.page_title);
        pageTitle.setText(userType.equals("Entrant")? "Entrants" : "Organizers");

        usersContainer = findViewById(R.id.profiles_container_organizer);
        userRepository = new UserRepository();

        usersScrollView = findViewById(R.id.profiles_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        usersScrollView.setVisibility(View.GONE);

        loadProfilesFromFirebase();
    }

    /**
     * Fetches users from Firebase based on type and displays them.
     * <p>
     * Makes an asynchronous call to the event repository to retrieve all applicable
     * users. On success, the users are passed to {@link #displayUsers(ArrayList)}
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
                    displayUsers(users);
                }
            }

            @Override
            public void onFailure(Exception e) {
                showEmptyState();
            }
        });
    }

    /**
     * Displays a list of users as card views in the users container.
     * <p>
     * Clears any existing user cards and dynamically inflates new card views
     * for each user in the list. Each card shows the user and is clickable
     * to open event details.
     * </p>
     *
     * @param users The list of users to display
     */
    private void displayUsers(ArrayList<User> users) {
        emptyState.setVisibility(View.GONE);
        usersScrollView.setVisibility(View.VISIBLE);
        usersContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (User user : users) {
            View userCard = inflater.inflate(R.layout.profile_card_admin_item, usersContainer, false);

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

            usersContainer.addView(userCard);
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
     * Shows the empty state layout with a message and hides the users scroll view.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        usersScrollView.setVisibility(View.GONE);
    }
}