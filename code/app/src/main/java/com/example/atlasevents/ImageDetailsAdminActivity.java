package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.utils.ImageUploader;

/**
 * Activity for displaying detailed information about an event.
 * <p>
 * This activity shows image details including the event name and organizer.
 * It also provides functionality to delete images.
 * </p>
 * <p>
 * The event object is passed to this activity via an Intent extra using the
 * {@link #EventKey} identifier.
 * </p>
 *
 * @see Event
 */
public class ImageDetailsAdminActivity extends AppCompatActivity {

    /**
     * Key used to pass the Event object through Intent extras.
     * This constant should be used when starting this activity to include
     * the event data in the intent.
     */
    public static final String EventKey = "com.example.atlasevents.EVENT";

    private EventRepository eventRepository;
    private Event currentEvent;
    private Entrant currentEntrant;

    ImageUploader uploader;

    private TextView eventNameTextView, organizerNameTextView;
    private ImageView eventImageView, backArrow, deleteButton;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components and populates them with event data retrieved
     * from the Intent extras. Sets up click listeners for the delete and back
     * arrow buttons.
     * </p>
     * <p>
     * The event object is retrieved using {@link #EventKey} and its details are
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
        setContentView(R.layout.admin_image_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventRepository = new EventRepository();
        uploader = new ImageUploader();

        eventNameTextView = findViewById(R.id.eventName);
        organizerNameTextView = findViewById(R.id.organizerName);
        eventImageView = findViewById(R.id.eventImage);
        backArrow = findViewById(R.id.back_arrow);
        eventImageView = findViewById(R.id.eventImage);

        deleteButton = findViewById(R.id.delete_icon);

        loadData();
        setupListeners();
    }

    /**
     * Loads the event from the repositories.
     * <p>
     */
    private void loadData(){
        eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EventDetailsActivity", "Failed to fetch event", e);
                Toast.makeText(ImageDetailsAdminActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Displays image details on the screen.
     * <p>
     * Populates all text fields and loads the event image using Glide.
     * </p>
     *
     * @param event The {@link Event} object containing event information.
     */
    private void displayEventDetails(Event event) {
        eventNameTextView.setText(event.getEventName());
        organizerNameTextView.setText(event.getOrganizer().getName());
        Glide.with(this).load(event.getImageUrl()).into(eventImageView);
        eventImageView.setVisibility(View.VISIBLE);
    }

    /**
     * Sets up click listeners for UI interactions such as the back arrow
     * and the delete button.
     */
    private void setupListeners() {
        backArrow.setOnClickListener(view -> finish());
        deleteButton.setOnClickListener(view -> {
            uploader.deleteImage(currentEvent.getImageUrl(), new ImageUploader.DeleteCallback() {
                @Override
                public void onSuccess() {
                    currentEvent.setImageUrl("");
                    eventRepository.updateEvent(currentEvent, success -> {
                        if (success) {
                            finish();
                            Toast.makeText(ImageDetailsAdminActivity.this, "Image deleted succesfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ImageDetailsAdminActivity.this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onFailure(String error) {
                }
            });
        });
    }
}
