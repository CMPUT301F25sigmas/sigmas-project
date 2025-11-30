package com.example.atlasevents;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atlasevents.data.UserRepository;

import java.util.Objects;

/**
 * Activity for displaying detailed information about a user.
 * <p>
 * This activity shows user details including the user name, user email and
 * phone number if applicable. It also provides functionality to delete users.
 * </p>
 * <p>
 * The user object is passed to this activity via an Intent extra using the
 * {@link #UserKey} identifier.
 * </p>
 *
 * @see User
 */
public class UserDetailsAdminActivity extends AppCompatActivity {

    /**
     * Key used to pass the User object through Intent extras.
     * This constant should be used when starting this activity to include
     * the user data in the intent.
     */
    public static final String UserKey = "com.example.atlasevents.USER";

    private UserRepository userRepository;
    private TextView userNameTextView, userEmailTextView, userPhoneTextView;
    private ImageView backArrow, deleteButton;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components and populates them with user data retrieved
     * from the Intent extras. Sets up click listeners for the delete and back
     * arrow buttons.
     * </p>
     * <p>
     * The user object is retrieved using {@link #UserKey} and its details are
     * displayed including:
     * </p>
     * <ul>
     *   <li>User name</li>
     *   <li>User email</li>
     *   <li>Phone number (if applicable)</li>
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

        userNameTextView = findViewById(R.id.userName);
        userEmailTextView = findViewById(R.id.userEmail);
        userPhoneTextView = findViewById(R.id.userPhone);

        backArrow = findViewById(R.id.back_arrow);
        deleteButton = findViewById(R.id.delete_icon);

        userRepository.getUser(Objects.requireNonNull(getIntent().getSerializableExtra(UserKey)).toString(), this::displayUserDetails);
        setupListeners();
    }

    /**
     * Displays user details on the screen.
     * <p>
     * Populates all text fields.
     * </p>
     *
     * @param user The {@link User} object containing user information.
     */
    private void displayUserDetails(User user) {
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
        deleteButton.setOnClickListener(view -> userRepository.deleteUser(Objects.requireNonNull(getIntent().getSerializableExtra(UserKey)).toString()));
    }
}
