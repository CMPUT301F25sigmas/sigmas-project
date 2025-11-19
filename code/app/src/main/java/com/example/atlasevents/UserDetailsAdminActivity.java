package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;

/**
 * Activity for displaying detailed information about an event.
 * <p>
 * This activity shows image details including the event name and organizer.
 * It also provides functionality to delete images.
 * </p>
 * <p>
 * The event object is passed to this activity via an Intent extra using the
 * {@link #UserKey} identifier.
 * </p>
 *
 * @see Event
 */
public class UserDetailsAdminActivity extends AppCompatActivity {

    /**
     * Key used to pass the Event object through Intent extras.
     * This constant should be used when starting this activity to include
     * the event data in the intent.
     */
    public static final String UserKey = "com.example.atlasevents.USER";

    private UserRepository userRepository;
    private TextView pageTitle, userNameTextView, userEmailTextView, userPhoneTextView;
    private ImageView backArrow, deleteButton;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components and populates them with event data retrieved
     * from the Intent extras. Sets up click listeners for the delete and back
     * arrow buttons.
     * </p>
     * <p>
     * The event object is retrieved using {@link #UserKey} and its details are
     * displayed including:
     * </p>
     * <ul>
     *   <li>Event name</li>
     *   <li>Organizer name</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_user_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userRepository = new UserRepository();

        pageTitle = findViewById(R.id.page_title);
        userNameTextView = findViewById(R.id.userName);
        userEmailTextView = findViewById(R.id.userEmail);
        userPhoneTextView = findViewById(R.id.userPhone);

        backArrow = findViewById(R.id.back_arrow);
        deleteButton = findViewById(R.id.delete_icon);

        userRepository.getUser(getIntent().getSerializableExtra(UserKey).toString(), this::displayUserDetails);
        setupListeners();
    }

    /**
     * Displays image details on the screen.
     * <p>
     * Populates all text fields and loads the event image using Glide.
     * </p>
     *
     * @param user The {@link Event} object containing event information.
     */
    private void displayUserDetails(User user) {
        pageTitle.setText(user.getUserType());
        userNameTextView.setText(user.getName());
        userEmailTextView.setText(user.getEmail());
        if(!user.getPhoneNumber().isEmpty()){
            userPhoneTextView.setText(user.getPhoneNumber());
            LinearLayout userPhoneLayout = findViewById(R.id.userPhoneLayout);
            userPhoneLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets up click listeners for UI interactions such as the back arrow
     * and the delete button.
     */
    private void setupListeners() {
        backArrow.setOnClickListener(view -> finish());
        deleteButton.setOnClickListener(view -> {
            userRepository.deleteUser(getIntent().getSerializableExtra(UserKey).toString());
        });
    }
}
