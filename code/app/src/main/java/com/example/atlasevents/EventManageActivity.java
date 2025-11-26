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
 * image, and waitlist details. It allows organizers to view different entrant lists
 * (waitlist, chosen, enrolled, cancelled) and download them as CSV files.
 * Includes lottery functionality with registration date validation and countdown timer.
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
 * @version 2.0
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

    /** Service for handling lottery operations. */
    private LotteryService lotteryService;

    /** Text view displaying the name of the event. */
    private TextView eventNameTextView;

    /** Text view displaying the current number of entrants on the waitlist. */
    private TextView waitlistCountTextView;

    /** Text view displaying the number of chosen entrants. */
    private TextView chosenCountTextView;

    /** Text view displaying the number of cancelled entrants. */
    private TextView cancelledCountTextView;

    /** Text view displaying the number of final enrolled entrants. */
    private TextView finalEnrolledCountTextView;

    /** Text view showing the scheduled date of the event. */
    private TextView dateTextView;

    /** Text view showing the event's location or address. */
    private TextView locationTextView;

    /** Text view showing lottery status information. */
    private TextView lotteryStatusText;

    /** Text view showing countdown timer until lottery becomes available. */
    private TextView timerTextView;

    /** Text view showing the title of the currently displayed list. */
    private TextView listTitleTextView;

    /** Image view displaying the event poster or default image. */
    private ImageView eventImageView;

    /** Recycler view for listing all entrants currently displayed. */
    private RecyclerView entrantsRecyclerView;

    /** Adapter for populating the entrant list in the recycler view. */
    private EntrantRecyclerAdapter entrantAdapter;

    /** Card view container for the list display section. */
    private CardView waitingListCard;

    /** Card view container for the lottery timer section. */
    private CardView lotteryTimerCard;

    /** Local list holding entrants currently displayed. */
    private ArrayList<Entrant> entrantList;

    /** List used for CSV download functionality. */
    private ArrayList<Entrant> downloadableList;

    /** Button for drawing the lottery. */
    private Button drawLotteryButton;

    /** Button for sending notifications. */
    private Button notifyButton;

    /** Button for showing map. */
    private Button showMapButton;

    /** Progress bar for lottery operations. */
    private ProgressBar lotteryProgressBar;

    /** Back button for navigation. */
    private ImageButton backButton;

    /** Download button for CSV export. */
    private ImageView downloadButton;

    /** Countdown timer for lottery availability. */
    private CountDownTimer countDownTimer;

    /** Current event being managed. */
    private Event currentEvent;

    /** Current event name for CSV file naming. */
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

        eventRepository = new EventRepository();
        lotteryService = new LotteryService();

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupListCardListeners();

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
        listTitleTextView = findViewById(R.id.listTitle);
        eventImageView = findViewById(R.id.eventPoster);
        waitingListCard = findViewById(R.id.waitingListViewCard);
        lotteryTimerCard = findViewById(R.id.lotteryTimerCard);
        entrantsRecyclerView = findViewById(R.id.entrantsRecyclerView);
        drawLotteryButton = findViewById(R.id.drawLotteryButton);
        notifyButton = findViewById(R.id.notifyButton);
        showMapButton = findViewById(R.id.showMapButton);
        lotteryProgressBar = findViewById(R.id.lotteryProgressBar);
        backButton = findViewById(R.id.backButton);
        downloadButton = findViewById(R.id.downloadButton);

        entrantList = new ArrayList<>();
        downloadableList = new ArrayList<>();
    }

    /**
     * Sets up click listeners for interactive elements.
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        drawLotteryButton.setOnClickListener(v -> showLotteryConfirmationDialog());

        notifyButton.setOnClickListener(v -> showNotificationOptions());

        showMapButton.setOnClickListener(v -> showEventMap());

        downloadButton.setOnClickListener(v -> downloadCurrentListAsCSV());
    }

    /**
     * Sets up the RecyclerView for displaying entrant lists.
     */
    private void setupRecyclerView() {
        entrantAdapter = new EntrantRecyclerAdapter(entrantList);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        entrantsRecyclerView.setAdapter(entrantAdapter);
    }

    /**
     * Sets up click listeners for the list card buttons.
     */
    private void setupListCardListeners() {
        LinearLayout waitingListButton = findViewById(R.id.WaitingListButton);
        LinearLayout enrolledButton = findViewById(R.id.enrolledButton);
        LinearLayout cancelledButton = findViewById(R.id.cancelledButton);
        LinearLayout chosenButton = findViewById(R.id.chosenButton);

        waitingListButton.setOnClickListener(view -> {
            setListVisibility(false, true, false, false);
            loadData();
        });

        enrolledButton.setOnClickListener(view -> {
            setListVisibility(false, false, false, true);
            loadData();
        });

        cancelledButton.setOnClickListener(view -> {
            setListVisibility(false, false, true, false);
            loadData();
        });

        chosenButton.setOnClickListener(view -> {
            setListVisibility(true, false, false, false);
            loadData();
        });
    }

    /**
     * Sets the visibility state for all lists.
     *
     * @param chosen Whether chosen list should be visible
     * @param waitlist Whether waitlist should be visible
     * @param cancelled Whether cancelled list should be visible
     * @param enrolled Whether enrolled list should be visible
     */
    private void setListVisibility(boolean chosen, boolean waitlist, boolean cancelled, boolean enrolled) {
        chosenVisible.set(chosen);
        waitlistVisible.set(waitlist);
        cancelledVisible.set(cancelled);
        enrolledVisible.set(enrolled);
    }

    /**
     * Loads event data from Firestore using the provided event ID.
     * <p>
     * Retrieves the event details such as name, date, location, image,
     * and all entrant lists. Once loaded, the method updates the UI elements and
     * populates the appropriate entrant list based on visibility settings.
     * </p>
     * <p>
     * If the event retrieval fails, a toast message is displayed and the
     * activity is closed.
     * </p>
     */
    private void loadData() {
        String eventId = getIntent().getSerializableExtra(EventKey).toString();

        eventRepository.getEventById(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                eventName = event.getEventName();
                updateEventUI(event);
                updateLotteryUI(event);
                startLotteryTimerIfNeeded(event);
                displayCurrentList(event);

                // Start cooldown countdown if needed
                startCooldownCountdownIfNeeded(event);
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
     * Starts a periodic update for cooldown countdown if lottery is in cooldown
     */
    private void startCooldownCountdownIfNeeded(Event event) {
        if (lotteryService.isInCooldownPeriod(event)) {
            // Update UI every minute to refresh cooldown countdown
            new CountDownTimer(Long.MAX_VALUE, 60000) { // Update every minute
                @Override
                public void onTick(long millisUntilFinished) {
                    if (currentEvent != null) {
                        updateLotteryUI(currentEvent);
                        startLotteryTimerIfNeeded(currentEvent);
                    }
                }

                @Override
                public void onFinish() {
                    // Timer runs indefinitely until canceled
                }
            }.start();
        }
    }

    /**
     * Updates the UI with event details.
     *
     * @param event The event to display
     */
    private void updateEventUI(Event event) {
        // Populate event details
        eventNameTextView.setText(event.getEventName());
        dateTextView.setText(event.getDateFormatted());
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
     * Displays the appropriate entrant list based on visibility settings.
     *
     * @param event The event with entrant lists
     */
    private void displayCurrentList(Event event) {
        String listTitle = "";
        ArrayList<Entrant> listToDisplay = null;

        if (chosenVisible.get() && event.getInviteList() != null) {
            listTitle = "Chosen Entrants";
            listToDisplay = event.getInviteList().getWaitList();
            downloadableList = listToDisplay;
        } else if (cancelledVisible.get() && event.getDeclinedList() != null) {
            listTitle = "Cancelled Entrants";
            listToDisplay = event.getDeclinedList().getWaitList();
            downloadableList = listToDisplay;
        } else if (enrolledVisible.get() && event.getAcceptedList() != null) {
            listTitle = "Enrolled Entrants";
            listToDisplay = event.getAcceptedList().getWaitList();
            downloadableList = listToDisplay;
        } else if (waitlistVisible.get() && event.getWaitlist() != null) {
            listTitle = "Waiting List";
            listToDisplay = event.getWaitlist().getWaitList();
            downloadableList = listToDisplay;
        }

        // Update list title
        listTitleTextView.setText(listTitle);

        // Display the list if available
        if (listToDisplay != null && !listToDisplay.isEmpty()) {
            entrantAdapter.setEntrants(listToDisplay);
            waitingListCard.setVisibility(View.VISIBLE);
            downloadButton.setVisibility(View.VISIBLE);
        } else {
            entrantAdapter.setEntrants(new ArrayList<>());
            waitingListCard.setVisibility(View.GONE);
            downloadButton.setVisibility(View.GONE);
            if (!listTitle.isEmpty()) {
                Toast.makeText(this, "No entrants in " + listTitle.toLowerCase(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Updates the lottery-related UI elements with cooldown support.
     *
     * @param event The event to check for lottery availability
     */
    private void updateLotteryUI(Event event) {
        boolean lotteryAvailable = lotteryService.isLotteryAvailable(event);
        int availableSlots = calculateAvailableSlots(event);
        int waitlistSize = event.getWaitlist() != null ? event.getWaitlist().size() : 0;
        int acceptedCount = event.getAcceptedList() != null ? event.getAcceptedList().size() : 0;

        // NEW: Check cooldown period
        boolean inCooldown = lotteryService.isInCooldownPeriod(event);
        long cooldownHoursRemaining = lotteryService.getCooldownHoursRemaining(event);

        // Update lottery status text with cooldown info
        String statusText = String.format(
                "Event: %s\n" +
                        "Entrant Limit: %d\n" +
                        "Accepted: %d\n" +
                        "Available Slots: %d\n" +
                        "Waitlist Size: %d\n" +
                        "Lottery Available: %s\n" +
                        "Registration End: %s\n" +
                        "Cooldown Active: %s" +
                        (inCooldown ? "\nCooldown Ends In: %d hours" : ""),
                event.getEventName(),
                event.getEntrantLimit(),
                acceptedCount,
                availableSlots,
                waitlistSize,
                lotteryAvailable ? "Yes" : "No",
                event.getRegEndDate() != null ? event.getRegEndDate() : "Not set",
                inCooldown ? "Yes" : "No",
                cooldownHoursRemaining
        );

        lotteryStatusText.setText(statusText);

        // NEW: Enhanced lottery button logic with cooldown
        boolean canDrawLottery = lotteryAvailable &&
                availableSlots > 0 &&
                waitlistSize > 0 &&
                !inCooldown;

        drawLotteryButton.setEnabled(canDrawLottery);
        drawLotteryButton.setAlpha(canDrawLottery ? 1.0f : 0.6f);

        // Update button text based on availability
        if (!canDrawLottery) {
            if (!lotteryAvailable) {
                drawLotteryButton.setText("Lottery Not Available");
            } else if (inCooldown) {
                drawLotteryButton.setText(String.format("Cooldown: %dh", cooldownHoursRemaining));
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
     * Starts the countdown timer if lottery is not yet available or in cooldown.
     *
     * @param event The event to check
     */
    private void startLotteryTimerIfNeeded(Event event) {
        boolean lotteryAvailable = lotteryService.isLotteryAvailable(event);
        boolean inCooldown = lotteryService.isInCooldownPeriod(event);

        // Show timer if either registration period hasn't ended OR lottery is in cooldown
        if (lotteryAvailable && !inCooldown) {
            lotteryTimerCard.setVisibility(View.GONE);
            return;
        }

        long timeRemaining;
        String timerPrefix;

        if (inCooldown) {
            // Show cooldown countdown
            timeRemaining = lotteryService.getCooldownHoursRemaining(event) * 60 * 60 * 1000;
            timerPrefix = "Lottery cooldown: ";
        } else {
            // Show registration period countdown
            timeRemaining = lotteryService.getTimeUntilLotteryAvailable(event);
            timerPrefix = "Lottery available in: ";
        }

        if (timeRemaining > 0) {
            lotteryTimerCard.setVisibility(View.VISIBLE);
            startCountdownTimer(timeRemaining, timerPrefix);
        } else {
            lotteryTimerCard.setVisibility(View.GONE);
        }
        if (timeRemaining > 0) {
            lotteryTimerCard.setVisibility(View.VISIBLE);
            startCountdownTimer(timeRemaining, timerPrefix); // Updated call
        } else {
            lotteryTimerCard.setVisibility(View.GONE);
        }
    }

    /**
     * Starts the countdown timer for lottery availability or cooldown.
     *
     * @param millisUntilFinished Time remaining in milliseconds
     * @param timerPrefix Prefix text for the timer display
     */
    private void startCountdownTimer(long millisUntilFinished, String timerPrefix) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millisUntilFinished, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerText(millisUntilFinished, timerPrefix);
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Lottery available now!");
                // Refresh UI after timer finishes
                new Handler().postDelayed(() -> {
                    if (currentEvent != null) {
                        updateLotteryUI(currentEvent);
                        lotteryTimerCard.setVisibility(View.GONE);
                        loadData(); // Reload to get updated state
                    }
                }, 2000);
            }
        }.start();
    }

    /**
     * Updates the timer text with formatted time.
     *
     * @param millisUntilFinished Time remaining in milliseconds
     * @param timerPrefix Prefix text for the timer display
     */
    private void updateTimerText(long millisUntilFinished, String timerPrefix) {
        long days = millisUntilFinished / (1000 * 60 * 60 * 24);
        long hours = (millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (millisUntilFinished % (1000 * 60)) / 1000;

        String timerText;
        if (days > 0) {
            timerText = String.format(timerPrefix + "%dd %02dh %02dm %02ds",
                    days, hours, minutes, seconds);
        } else if (hours > 0) {
            timerText = String.format(timerPrefix + "%02dh %02dm %02ds",
                    hours, minutes, seconds);
        } else {
            timerText = String.format(timerPrefix + "%02dm %02ds", minutes, seconds);
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
                        "Selected entrants will receive invitation notifications.\n\n" +
                        "Note: Lottery will be locked for 24 hours after drawing.",
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
     * Downloads the currently displayed list as a CSV file.
     */
    private void downloadCurrentListAsCSV() {
        if (downloadableList == null || downloadableList.isEmpty()) {
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show();
            return;
        }

        String listType = "";
        if (waitlistVisible.get()) listType = "waitList";
        if (cancelledVisible.get()) listType = "cancelledList";
        if (enrolledVisible.get()) listType = "enrolledList";
        if (chosenVisible.get()) listType = "chosenList";

        String fileName = eventName + "_" + listType + ".csv";
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

            // Write CSV header
            writer.write("Name,Email,Phone Number");
            writer.newLine();

            // Write entrant data
            for (int i = 0; i < downloadableList.size(); i++) {
                Entrant entrant = downloadableList.get(i);
                writer.write(entrant.getName() != null ? entrant.getName() : "");
                writer.write(",");
                writer.write(entrant.getEmail() != null ? entrant.getEmail() : "");
                writer.write(",");
                writer.write(entrant.getPhoneNumber() != null ? entrant.getPhoneNumber() : "");
                writer.newLine();
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "Saved to Downloads: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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

    /**
     * Shows the event location on a map.
     */
    private void showEventMap() {
        if (currentEvent == null || currentEvent.getAddress() == null) {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Implement map display functionality
        Toast.makeText(this, "Map would open for: " + currentEvent.getAddress(), Toast.LENGTH_SHORT).show();
    }
}