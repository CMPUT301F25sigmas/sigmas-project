package com.example.atlasevents;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity for organizers to manage an existing event.
 * <p>
 * This activity displays event information such as its name, date, location,
 * image, and waitlist details. It also allows organizers to view a list of
 * entrants currently on the event’s waitlist through a {@link RecyclerView}.
 * </p>
 *
 * <p>
 * The event is identified by an event ID passed through an {@link android.content.Intent}
 * extra using the constant {@link #EventKey}. The event data is fetched from Firestore
 * via the {@link EventRepository}.
 * </p>
 *
 * @see Event
 * @see EventRepository
 * @see EntrantRecyclerAdapter
 */
public class EventManageActivity extends AppCompatActivity {

    /**
     * Key used for passing the event ID via an Intent extra.
     * <p>
     * Activities that need to open this screen should attach the event ID
     * using {@code intent.putExtra(EventManageActivity.EventKey, event.getId())}.
     * </p>
     */
    public static final String EventKey = "com.example.atlasevents.EVENT";

    /** Repository for accessing and managing event data in Firestore. */
    private EventRepository eventRepository;

    /** Text view displaying the name of the event. */
    private TextView eventNameTextView;

    /** Text view displaying the current number of entrants on the waitlist. */
    private TextView waitlistCountTextView;

    /** Text view showing the scheduled date of the event. */
    private TextView dateTextView;

    /** Text view showing the event's location or address. */
    private TextView locationTextView;

    /** Image view displaying the event poster or default image. */
    private ImageView eventImageView;

    /** Recycler view for listing all entrants currently on the event waitlist. */
    private RecyclerView entrantsRecyclerView;

    /** Adapter for populating the entrant list in the recycler view. */
    private EntrantRecyclerAdapter entrantAdapter;

    /** Card view container for the waitlist section (hidden if waitlist is empty). */
    private CardView waitingListCard;

    /** Local list holding entrants currently on the waitlist. */
    private ArrayList<Entrant> entrantList;
    private ArrayList<Entrant> downloadableList;
    private String eventName;

    /**
     * These booleans are for which list is visible, controlled by clicking on the list cards
     */
    private final AtomicBoolean chosenVisible = new AtomicBoolean(false);
    private final AtomicBoolean waitlistVisible = new AtomicBoolean(false);
    private final AtomicBoolean cancelledVisible = new AtomicBoolean(false);
    private final AtomicBoolean enrolledVisible = new AtomicBoolean(false);



    /**
     * Called when the activity is first created.
     * <p>
     * Sets up the layout, initializes UI elements, and prepares the
     * {@link RecyclerView} for displaying entrants. It also triggers
     * the initial load of event data from Firestore using {@link #loadData()}.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied. Otherwise, it is {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_event);

        // Apply window insets for modern edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /**
         * back button
         */
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view ->{
            finish();
        });
        Button drawLotteryButton = findViewById(R.id.drawLotteryButton);
        drawLotteryButton.setOnClickListener(view ->{
            //draw lottery
        });

        Button showMapButton = findViewById(R.id.showMapButton);
        showMapButton.setOnClickListener(view -> {
            Intent intent = new Intent(EventManageActivity.this, ManageEventMapActivity.class);
            startActivity(intent);
        });

        ImageView downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(view -> {

            String listType = "";
            if (waitlistVisible.get()){listType = "waitList";}
            if (cancelledVisible.get()){listType = "cancelledList";}
            if (enrolledVisible.get()){listType = "enrolledList";}
            if (chosenVisible.get()){listType = "chosenList";}

            String fileName = eventName +"_"+ listType + ".csv";
            ContentResolver resolver = getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            try {
                OutputStream outputStream = resolver.openOutputStream(uri);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                for (int i = 0; i < downloadableList.size(); i++) {
                    writer.write(downloadableList.get(i).getName());
                    writer.write(",");
                    writer.write(downloadableList.get(i).getEmail());
                    writer.write(",");
                    writer.write(downloadableList.get(i).getPhoneNumber());
                    writer.newLine();
                }

                writer.flush();
                writer.close();

                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });


        eventRepository = new EventRepository();

        eventNameTextView = findViewById(R.id.eventTitle);
        waitlistCountTextView = findViewById(R.id.waitingListCount);
        dateTextView = findViewById(R.id.eventDate);
        locationTextView = findViewById(R.id.eventLocation);
        eventImageView = findViewById(R.id.eventPoster);
        waitingListCard = findViewById(R.id.waitingListViewCard);
        entrantsRecyclerView = findViewById(R.id.entrantsRecyclerView);
        LinearLayout waitingListButton = findViewById(R.id.WaitingListButton);
        LinearLayout enrolledButton = findViewById(R.id.enrolledButton);
        LinearLayout cancelledButton = findViewById(R.id.cancelledButton);
        LinearLayout chosenButton = findViewById(R.id.chosenButton);




        entrantList = new ArrayList<>();
        downloadableList = new ArrayList<>();
        entrantAdapter = new EntrantRecyclerAdapter(entrantList);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        entrantsRecyclerView.setAdapter(entrantAdapter);
        loadData();

        /**
         * Listeners for list cards
         */
        waitingListButton.setOnClickListener(view ->{
            chosenVisible.set(false);
            waitlistVisible.set(true);
            cancelledVisible.set(false);
            enrolledVisible.set(false);
            loadData();
        });
        enrolledButton.setOnClickListener(view ->{
            chosenVisible.set(false);
            waitlistVisible.set(false);
            cancelledVisible.set(false);
            enrolledVisible.set(true);
            loadData();
        });
        cancelledButton.setOnClickListener(view ->{
            chosenVisible.set(false);
            waitlistVisible.set(false);
            cancelledVisible.set(true);
            enrolledVisible.set(false);
            loadData();
        });
        chosenButton.setOnClickListener(view ->{
            chosenVisible.set(true);
            waitlistVisible.set(false);
            cancelledVisible.set(false);
            enrolledVisible.set(false);
            loadData();
        });
    }

    /**
     * Loads event data from Firestore using the provided event ID.
     * <p>
     * Retrieves the event details such as name, date, location, image,
     * and waitlist. Once loaded, the method updates the UI elements and
     * populates the entrant list if applicable based on listVisible booleans.
     * </p>
     * <p>
     * If the event retrieval fails, a toast message is displayed and the
     * activity is closed.
     * </p>
     */
    private void loadData() {
        eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(),
                new EventRepository.EventCallback() {
                    @Override
                    public void onSuccess(Event event) {
                        // Populate event details
                        eventNameTextView.setText(event.getEventName());
                        waitlistCountTextView.setText(String.valueOf(
                                event.getWaitlist() != null ? event.getWaitlist().size() : 0));
                        dateTextView.setText(event.getDateFormatted());
                        locationTextView.setText(event.getAddress());

                        // Load event image using Glide
                        if (!event.getImageUrl().isEmpty()) {
                            Glide.with(EventManageActivity.this)
                                    .load(event.getImageUrl())
                                    .into(eventImageView);
                        } else {
                            eventImageView.setImageResource(R.drawable.poster);
                        }

                        // Display waitlist or other lists if available
                        if (chosenVisible.get() &&
                                event.getInviteList() != null &&
                                event.getInviteList().getWaitList() != null &&
                                !event.getInviteList().getWaitList().isEmpty()) {
                            entrantAdapter.setEntrants(event.getInviteList().getWaitList());
                            waitingListCard.setVisibility(View.VISIBLE);
                            downloadableList = event.getInviteList().getWaitList(); //set downloadable list
                        }else if (cancelledVisible.get() &&
                                event.getDeclinedList() != null &&
                                event.getDeclinedList().getWaitList() != null &&
                                !event.getDeclinedList().getWaitList().isEmpty()) {
                            entrantAdapter.setEntrants(event.getDeclinedList().getWaitList());
                            waitingListCard.setVisibility(View.VISIBLE);
                            downloadableList = event.getDeclinedList().getWaitList();

                        } else if (enrolledVisible.get() &&
                                event.getAcceptedList() != null &&
                                event.getAcceptedList().getWaitList() != null &&
                                !event.getAcceptedList().getWaitList().isEmpty()) {
                            entrantAdapter.setEntrants(event.getAcceptedList().getWaitList());
                            waitingListCard.setVisibility(View.VISIBLE);
                            downloadableList = event.getAcceptedList().getWaitList();

                        } else if (waitlistVisible.get() &&
                                event.getWaitlist() != null &&
                                event.getWaitlist().getWaitList() != null &&
                                !event.getWaitlist().getWaitList().isEmpty()) {
                            entrantAdapter.setEntrants(event.getWaitlist().getWaitList());
                            waitingListCard.setVisibility(View.VISIBLE);
                            downloadableList = event.getWaitlist().getWaitList();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("EventManageActivity", "Failed to fetch event", e);
                        Toast.makeText(EventManageActivity.this,
                                "Failed to load event", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
}
