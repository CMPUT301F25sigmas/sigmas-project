package com.example.atlasevents;

import android.app.AlertDialog;
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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
 * @see LotteryService
 * @version 1.1
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
    /** Service for handling lottery operations. */
    private LotteryService lotteryService;
    /** Text view displaying the number of chosen entrants. */
    private TextView chosenCountTextView;

    /** Text view displaying the number of cancelled entrants. */
    private TextView cancelledCountTextView;

    /** Text view displaying the number of final enrolled entrants. */
    private TextView finalEnrolledCountTextView;

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



    /** Text view showing lottery status information. */
    private TextView lotteryStatusText;

    /** Text view showing countdown timer until lottery becomes available. */
    private TextView timerTextView;

    /** Card view container for the lottery timer section. */
    private CardView lotteryTimerCard;

    /**Button for drawing the lottery */
    private Button drawLotteryButton;

    /** Button for sending notifications. */
    private Button notifyButton;

    /** Progress bar for lottery operations. */
    private ProgressBar lotteryProgressBar;

    /** Back button for navigation. */
    private ImageButton backButton;

    /** Countdown timer for lottery availability. */
    private CountDownTimer countDownTimer;

    /** Current event being managed. */
    private Event currentEvent;

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

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadData();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel countdown timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
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

    /**
     * Initializes all UI views from the layout.
     */
    private void initializeViews() {
        eventNameTextView = findViewById(R.id.eventTitle);
        waitlistCountTextView = findViewById(R.id.waitingListCount);
        chosenCountTextView = findViewById(R.id.chosenCount);
        cancelledCountTextView = findViewById(R.id.cancelledCount);
        finalEnrolledCountTextView = findViewById(R.id.finalEnrolledCount);
        dateTextView = findViewById(R.id.eventDate);
        locationTextView = findViewById(R.id.eventLocation);
        lotteryStatusText = findViewById(R.id.lotteryStatusText);
        timerTextView = findViewById(R.id.timerTextView);
        eventImageView = findViewById(R.id.eventPoster);
        waitingListCard = findViewById(R.id.waitingListViewCard);
        lotteryTimerCard = findViewById(R.id.lotteryTimerCard);
        entrantsRecyclerView = findViewById(R.id.entrantsRecyclerView);
        drawLotteryButton = findViewById(R.id.drawLotteryButton);
        notifyButton = findViewById(R.id.notifyButton);
        lotteryProgressBar = findViewById(R.id.lotteryProgressBar);
        backButton = findViewById(R.id.backButton);
    }
        LinearLayout waitingListButton = findViewById(R.id.WaitingListButton);
        LinearLayout enrolledButton = findViewById(R.id.enrolledButton);
        LinearLayout cancelledButton = findViewById(R.id.cancelledButton);
        LinearLayout chosenButton = findViewById(R.id.chosenButton);




    /**
     * Sets up click listeners for interactive elements.
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        drawLotteryButton.setOnClickListener(v -> showLotteryConfirmationDialog());

        notifyButton.setOnClickListener(v -> showNotificationOptions());
    }

    /**
     * Sets up the RecyclerView for displaying the waitlist.
     */
    private void setupRecyclerView() {
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
        String eventId = getIntent().getSerializableExtra(EventKey).toString();
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
                    }
                        });
        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                updateEventUI(event);
                updateLotteryUI(event);
                startLotteryTimerIfNeeded(event);
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
    /**
     * Updates the UI with event details.
     *
     * @param event The event to display
     */
    private void updateEventUI(Event event) {
        // Populate event details
        eventNameTextView.setText(event.getEventName());
        dateTextView.setText(event.getDate());
        locationTextView.setText(event.getAddress());

        // Update count displays
        updateCountDisplays(event);

        // Load event image using Glide
        if (!event.getImageUrl().isEmpty()) {
            Glide.with(EventManageActivity.this)
                    .load(event.getImageUrl())
                    .into(eventImageView);
        } else {
            eventImageView.setImageResource(R.drawable.poster);
        }

        // Display waitlist if available
        if (event.getWaitlist() != null &&
                event.getWaitlist().getWaitList() != null &&
                !event.getWaitlist().getWaitList().isEmpty()) {
            entrantAdapter.setEntrants(event.getWaitlist().getWaitList());
            waitingListCard.setVisibility(View.VISIBLE);
        } else {
            waitingListCard.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the count displays for all entrant lists.
     *
     * @param event The event with entrant lists
     */
    private void updateCountDisplays(Event event) {
        int waitlistCount = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        int chosenCount = event.getInviteList() != null ? event.getInviteList().size() : 0;
        int cancelledCount = event.getDeclinedList() != null ? event.getDeclinedList().size() : 0;
        int finalEnrolledCount = event.getAcceptedList() != null ? event.getAcceptedList().size() : 0;

        waitlistCountTextView.setText(String.valueOf(waitlistCount));
        chosenCountTextView.setText(String.valueOf(chosenCount));
        cancelledCountTextView.setText(String.valueOf(cancelledCount));
        finalEnrolledCountTextView.setText(String.valueOf(finalEnrolledCount));
    }

    /**
     * Updates the lottery-related UI elements.
     *
     * @param event The event to check for lottery availability
     */
    private void updateLotteryUI(Event event) {
        boolean lotteryAvailable = lotteryService.isLotteryAvailable(event);
        int availableSlots = calculateAvailableSlots(event);
        int waitlistSize = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        int acceptedCount = event.getAcceptedList() != null ? event.getAcceptedList().size() : 0;

        // Update lottery status text
        String statusText = String.format(
                "Event: %s\n" +
                        "Entrant Limit: %d\n" +
                        "Accepted: %d\n" +
                        "Available Slots: %d\n" +
                        "Waitlist Size: %d\n" +
                        "Lottery Available: %s\n" +
                        "Registration End: %s",
                event.getEventName(),
                event.getEntrantLimit(),
                acceptedCount,
                availableSlots,
                waitlistSize,
                lotteryAvailable ? "Yes" : "No",
                event.getRegEndDate() != null ? event.getRegEndDate() : "Not set"
        );

        lotteryStatusText.setText(statusText);

        // Enable/disable lottery button based on availability
        boolean canDrawLottery = lotteryAvailable && availableSlots > 0 && waitlistSize > 0;
        drawLotteryButton.setEnabled(canDrawLottery);
        drawLotteryButton.setAlpha(canDrawLottery ? 1.0f : 0.6f);

        // Update button text based on availability
        if (!canDrawLottery) {
            if (!lotteryAvailable) {
                drawLotteryButton.setText("Lottery Not Available");
            } else if (availableSlots <= 0) {
                drawLotteryButton.setText("Event Full");
            } else if (waitlistSize <= 0) {
                drawLotteryButton.setText("Empty Waitlist");
            }
        } else {
            drawLotteryButton.setText("Draw Lottery");
        }
    }

    /**
     * Starts the countdown timer if lottery is not yet available.
     *
     * @param event The event to check
     */
    private void startLotteryTimerIfNeeded(Event event) {
        if (lotteryService.isLotteryAvailable(event)) {
            lotteryTimerCard.setVisibility(View.GONE);
            return;
        }

        long timeRemaining = lotteryService.getTimeUntilLotteryAvailable(event);
        if (timeRemaining > 0) {
            lotteryTimerCard.setVisibility(View.VISIBLE);
            startCountdownTimer(timeRemaining);
        } else {
            lotteryTimerCard.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the countdown timer for lottery availability.
     *
     * @param millisUntilFinished Time remaining in milliseconds
     */
    private void startCountdownTimer(long millisUntilFinished) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millisUntilFinished, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Lottery available now!");
                // Refresh UI after timer finishes
                new Handler().postDelayed(() -> {
                    if (currentEvent != null) {
                        updateLotteryUI(currentEvent);
                        lotteryTimerCard.setVisibility(View.GONE);
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
                }, 2000);
            }
        }.start();
    }

    /**
     * Updates the timer text with formatted time.
     *
     * @param millisUntilFinished Time remaining in milliseconds
     */
    private void updateTimerText(long millisUntilFinished) {
        long days = millisUntilFinished / (1000 * 60 * 60 * 24);
        long hours = (millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (millisUntilFinished % (1000 * 60)) / 1000;

        String timerText;
        if (days > 0) {
            timerText = String.format("Lottery available in: %dd %02dh %02dm %02ds",
                    days, hours, minutes, seconds);
        } else if (hours > 0) {
            timerText = String.format("Lottery available in: %02dh %02dm %02ds",
                    hours, minutes, seconds);
        } else {
            timerText = String.format("Lottery available in: %02dm %02ds", minutes, seconds);
        }

        timerTextView.setText(timerText);
    }

    /**
     * Calculates available slots for the lottery draw.
     *
     * @param event The event to calculate slots for
     * @return Number of available slots
     */
    private int calculateAvailableSlots(Event event) {
        int entrantLimit = event.getEntrantLimit();
        int acceptedCount = event.getAcceptedList() != null ? event.getAcceptedList().size() : 0;

        return Math.max(0, entrantLimit - acceptedCount);
    }

    /**
     * Shows confirmation dialog before drawing lottery.
     */
    private void showLotteryConfirmationDialog() {
        if (currentEvent == null) return;

        int availableSlots = calculateAvailableSlots(currentEvent);
        int waitlistSize = currentEvent.getWaitlist() != null ? currentEvent.getWaitlist().size() : 0;

        String message = String.format(
                "Draw lottery for '%s'?\n\n" +
                        "Available slots: %d\n" +
                        "Waitlist size: %d\n\n" +
                        "Selected entrants will receive invitation notifications.",
                currentEvent.getEventName(), availableSlots, waitlistSize
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Draw Lottery")
                .setMessage(message)
                .setPositiveButton("Draw Lottery", (dialog, which) -> {
                    executeLotteryDraw();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Executes the lottery draw process.
     */
    private void executeLotteryDraw() {
        if (currentEvent == null) return;

        setLotteryInProgress(true);

        lotteryService.drawLottery(currentEvent.getId(), new LotteryService.LotteryCallback() {
            @Override
            public void onLotteryCompleted(int entrantsSelected, String message) {
                runOnUiThread(() -> {
                    setLotteryInProgress(false);
                    showLotteryResult(true, entrantsSelected, message);
                    // Reload data to reflect changes
                    loadData();
                });
            }

            @Override
            public void onLotteryFailed(Exception exception) {
                runOnUiThread(() -> {
                    setLotteryInProgress(false);
                    showLotteryResult(false, 0, "Lottery failed: " + exception.getMessage());
                    updateLotteryUI(currentEvent);
                });
            }
        });
    }

    /**
     * Sets the lottery progress state.
     *
     * @param inProgress Whether lottery is in progress
     */
    private void setLotteryInProgress(boolean inProgress) {
        lotteryProgressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        drawLotteryButton.setEnabled(!inProgress);
        drawLotteryButton.setText(inProgress ? "Drawing Lottery..." : "Draw Lottery");
    }

    /**
     * Shows lottery result to the user.
     *
     * @param success Whether the lottery was successful
     * @param entrantsSelected Number of entrants selected
     * @param message Result message
     */
    private void showLotteryResult(boolean success, int entrantsSelected, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (success) {
            builder.setTitle("Lottery Completed")
                    .setMessage(message)
                    .setPositiveButton("OK", null);
        } else {
            builder.setTitle("Lottery Failed")
                    .setMessage(message)
                    .setPositiveButton("OK", null);
        }

        builder.show();
    }

    /**
     * Shows notification options for the organizer.
     */
    private void showNotificationOptions() {
        if (currentEvent == null) return;

        // Implement notification options based on your existing notification system
        // This could open a dialog or new activity for sending notifications
        Toast.makeText(this, "Notification options would open here", Toast.LENGTH_SHORT).show();
    }
}



