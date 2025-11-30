package com.example.atlasevents;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
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
import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.atlasevents.utils.MapWarmUpManager;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Repository for sending notifications. */
    private NotificationRepository notificationRepository;

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
        MapWarmUpManager.warmUp(getApplicationContext());

        // Apply window insets for modern edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventRepository = new EventRepository();
        lotteryService = new LotteryService();
        notificationRepository = new NotificationRepository();

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

        // Set up click listener for remove button
        entrantAdapter.setOnEntrantClickListener(entrant -> {
            showMoveToCancelledDialog(entrant);
        });

        // Set up long press listener for sending notifications
        entrantAdapter.setOnEntrantLongClickListener(entrant -> {
            showSendNotificationDialog(entrant);
            return true;
        });
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
        String eventId = getIntent().getStringExtra(EventKey);
        if (eventId == null) {
            Object extra = getIntent().getSerializableExtra(EventKey);
            if (extra != null) {
                eventId = extra.toString();
            }
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Only fetch the event from Firebase; we don't rely on Event methods for lists
        String finalEventId = eventId;
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Event event = snapshot.toObject(Event.class); // for other Event fields like name
                    if (event == null) {
                        Toast.makeText(this, "Failed to parse event", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentEvent = event;
                    eventName = currentEvent.getEventName();
                    MapWarmUpManager.cacheEntrantCoords(finalEventId, currentEvent.getEntrantCoords());
                    updateEventUI(currentEvent, snapshot);
                    updateLotteryUI(currentEvent);
                    startLotteryTimerIfNeeded(currentEvent);

                    //display lists directly from Firebase snapshot
                    displayCurrentListFromSnapshot(currentEvent, snapshot);
                    updateCountDisplays(currentEvent, snapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventManageActivity", "Failed to fetch event", e);
                    Toast.makeText(EventManageActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }


    /**
     * Updates the UI with event details.
     *
     * @param event The event to display
     */
    private void updateEventUI(Event event, DocumentSnapshot snapshot) {
        // Populate event details
        eventNameTextView.setText(event.getEventName());
        dateTextView.setText(event.getDateFormatted());
        locationTextView.setText(event.getAddress());

        // Update count displays
        if (snapshot != null) {
            updateCountDisplays(event,snapshot);
        }

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
    private void updateCountDisplays(Event event, DocumentSnapshot snapshot) {
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
    private void displayCurrentListFromSnapshot(Event event, DocumentSnapshot snapshot) {
        String listTitle = "";
        ArrayList<Entrant> listToDisplay = new ArrayList<>();

        if (chosenVisible.get()) {
            listTitle = "Chosen Entrants";
            if (event.getInviteList() != null) {
                listToDisplay = event.getInviteList().getWaitList();
            }
            downloadableList = listToDisplay;
        } else if (cancelledVisible.get()) {
            listTitle = "Cancelled Entrants";
            if (event.getDeclinedList() != null) {
                listToDisplay = event.getDeclinedList().getWaitList();
            }
            downloadableList = listToDisplay;
        } else if (enrolledVisible.get()) {
            listTitle = "Enrolled Entrants";
            if (event.getAcceptedList() != null) {
                listToDisplay = event.getAcceptedList().getWaitList();
            }
            downloadableList = listToDisplay;
        } else if (waitlistVisible.get()) {
            listTitle = "Waiting List";
            if (event.getWaitlist() != null) {
                listToDisplay = event.getWaitlist().getWaitList();
            }
            downloadableList = listToDisplay;
        }

        // Update UI
        listTitleTextView.setText(listTitle);
        if (!listToDisplay.isEmpty()) {
            entrantAdapter.setEntrants(listToDisplay);
            // Show remove button for enrolled, invite (chosen), and waitlist (but not cancelled list)
            boolean showRemove = enrolledVisible.get() || chosenVisible.get() || waitlistVisible.get();
            entrantAdapter.setShowRemoveButton(showRemove);
            waitingListCard.setVisibility(View.VISIBLE);
            downloadButton.setVisibility(View.VISIBLE);
        } else {
            entrantAdapter.setEntrants(new ArrayList<>());
            entrantAdapter.setShowRemoveButton(false);
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
        int pendingInvites = event.getInviteList() != null ? event.getInviteList().size() : 0;
        // NEW: Check cooldown period

        String entrantLimit = String.valueOf(event.getEntrantLimit());
        if (entrantLimit.equals("-1")){ entrantLimit = "No limit";}

        // Update lottery status text with cooldown info
        String statusText = String.format(
                "Event: %s\n" +
                        "Waitlist Limit: %s\n" +
                        "Accepted: %d\n" +
                        "Available Slots: %d\n"+
                        "Pending Invites: %d\n"+
                        "Waitlist Size: %d\n" +
                        "Lottery Available: %s\n" +
                        "Registration End: %s\n",
                event.getEventName(),
                entrantLimit,
                acceptedCount,
                availableSlots,
                pendingInvites,
                waitlistSize,
                lotteryAvailable ? "Yes" : "No",
                event.getRegEndDate() != null ? event.getRegEndDate() : "Not set"

        );

        lotteryStatusText.setText(statusText);

        // NEW: Enhanced lottery button logic with cooldown
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
        boolean lotteryAvailable = lotteryService.isLotteryAvailable(event);


        long timeRemaining;
        String timerPrefix;


            // Show registration period countdown
            timeRemaining = lotteryService.getTimeUntilLotteryAvailable(event);
            timerPrefix = "Lottery available in: ";


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
        int entrantLimit = event.getSlots();
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
        int pendingInvites = currentEvent.getInviteList() != null ? currentEvent.getInviteList().size() : 0;
        int totalToSelect = availableSlots + pendingInvites;
        String message;
        if (pendingInvites>0){message= String.format(
                "Re-sample lottery for '%s'?\n\n" + "Available slots: %d\n" +
                        "Pending invites: %d\n" + "Total to select: %d\n" +
                        "Waitlist size: %d\n\n" + "This will:\n" +
                        "• Cancel %d pending invitations\n" + "• Select %d new entrants from waitlist\n" +
                        "• Accepted entrants (%d) remain unaffected",
                currentEvent.getEventName(), availableSlots, pendingInvites, totalToSelect, waitlistSize,
                pendingInvites, totalToSelect,
                currentEvent.getAcceptedList() != null ? currentEvent.getAcceptedList().size() : 0
        );
        } else {
            message = String.format(
                    "Draw lottery for '%s'?\n\n" +
                            "Available slots: %d\n" +
                            "Waitlist size: %d\n\n" +
                            "Selected entrants will receive invitation notifications.",
                    currentEvent.getEventName(), availableSlots, waitlistSize
            );
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(pendingInvites > 0 ? "Re-sample Lottery" : "Draw Lottery")
                .setMessage(message)
                .setPositiveButton(pendingInvites > 0 ? "Re-sample" : "Draw Lottery", (dialog, which) -> {
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

        int pendingInvites = currentEvent.getInviteList() != null ? currentEvent.getInviteList().size() : 0;

        LotteryService.LotteryCallback lotteryCallback = new LotteryService.LotteryCallback() {
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
        };

        // Use re-sampling if there are pending invites, otherwise use normal lottery
        if (pendingInvites > 0) {
            lotteryService.resampleLottery(currentEvent.getId(), lotteryCallback);
        } else {
            lotteryService.drawLottery(currentEvent.getId(), lotteryCallback);
        }
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
        List<Entrant> listToDownload;

        String listType = "";
        if (waitlistVisible.get()) {
            listType = "waitlist";
            // Use the waitlist directly from the Event object
            listToDownload = currentEvent.getWaitlist() != null
                    ? currentEvent.getWaitlist().getWaitList()
                    : new ArrayList<>();
        } else if (cancelledVisible.get()) {
            listType = "cancelledList";
            listToDownload = downloadableList;
        } else if (enrolledVisible.get()) {
            listType = "enrolledList";
            listToDownload = downloadableList;
        } else if (chosenVisible.get()) {
            listType = "chosenList";
            listToDownload = downloadableList;
        } else {
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listToDownload.isEmpty()) {
            Toast.makeText(this, "No data to download", Toast.LENGTH_SHORT).show();
            return;
        }

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

        try (OutputStream outputStream = resolver.openOutputStream(uri);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            // CSV header
            writer.write("Name,Email,Phone Number");
            writer.newLine();

            // Write entrant data
            for (Entrant entrant : listToDownload) {
                writer.write(entrant.getName() != null ? entrant.getName() : "");
                writer.write(",");
                writer.write(entrant.getEmail() != null ? entrant.getEmail() : "");
                writer.write(",");
                writer.write(entrant.getPhoneNumber() != null ? entrant.getPhoneNumber() : "");
                writer.newLine();
            }

            Toast.makeText(this, "Saved to Downloads: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    /**
     * Shows notification options for the organizer.
     */
    private void showNotificationOptions() {
        if (currentEvent == null || currentEvent.getId() == null) {
            Toast.makeText(this, "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ComposeNotificationActivity.class);
        intent.putExtra("eventId", currentEvent.getId());
        intent.putExtra("eventName", currentEvent.getEventName());
        startActivity(intent);
    }

    /**
     * Shows the event location on a map.
     */
    private void showEventMap() {
        /*
        if (currentEvent == null || currentEvent.getAddress() == null) {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Implement map display functionality
        Toast.makeText(this, "Map would open for: " + currentEvent.getAddress(), Toast.LENGTH_SHORT).show();
                 */
        if (currentEvent == null) {
            return;
        }

        Intent intent = new Intent(this, ManageEventMapActivity.class);
        intent.putExtra(ManageEventMapActivity.EXTRA_EVENT_ID, currentEvent.getId());
        startActivity(intent);
    }

    /**
     * Shows a confirmation dialog to move an entrant to cancelled list.
     *
     * @param entrant The entrant to move
     */
    private void showMoveToCancelledDialog(Entrant entrant) {
        if (currentEvent == null || entrant == null) {
            return;
        }

        // Determine which list the entrant is currently in
        String sourceList = "current list";
        if (enrolledVisible.get()) {
            sourceList = "enrolled list";
        } else if (chosenVisible.get()) {
            sourceList = "invite list";
        } else if (waitlistVisible.get()) {
            sourceList = "waitlist";
        }

        String message = String.format(
                "Move '%s' (%s) from %s to cancelled list?",
                entrant.getName() != null ? entrant.getName() : "Unknown",
                entrant.getEmail() != null ? entrant.getEmail() : "Unknown",
                sourceList
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Move to Cancelled List")
                .setMessage(message)
                .setPositiveButton("Move", (dialog, which) -> {
                    moveEntrantToCancelled(entrant);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Moves an entrant from the current list (accepted, invite, or waitlist) to the declined (cancelled) list in Firestore.
     *
     * @param entrant The entrant to move
     */
    private void moveEntrantToCancelled(Entrant entrant) {
        if (currentEvent == null || entrant == null || currentEvent.getId() == null) {
            Toast.makeText(this, "Unable to move entrant", Toast.LENGTH_SHORT).show();
            return;
        }

        EntrantList sourceList = null;
        String sourceListName = "";
        EntrantList acceptedList = currentEvent.getAcceptedList() != null 
                ? currentEvent.getAcceptedList() 
                : new EntrantList();
        EntrantList inviteList = currentEvent.getInviteList() != null 
                ? currentEvent.getInviteList() 
                : new EntrantList();
        EntrantList waitlist = currentEvent.getWaitlist() != null 
                ? currentEvent.getWaitlist() 
                : new EntrantList();
        EntrantList declinedList = currentEvent.getDeclinedList() != null 
                ? currentEvent.getDeclinedList() 
                : new EntrantList();

        // Determine which list to remove from based on current visibility
        if (enrolledVisible.get()) {
            sourceList = acceptedList;
            sourceListName = "enrolled list";
        } else if (chosenVisible.get()) {
            sourceList = inviteList;
            sourceListName = "invite list";
        } else if (waitlistVisible.get()) {
            sourceList = waitlist;
            sourceListName = "waitlist";
        } else {
            Toast.makeText(this, "Cannot move from this list", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the entrant in the source list by email
        Entrant entrantToMove = null;
        for (int i = 0; i < sourceList.size(); i++) {
            Entrant e = sourceList.getEntrant(i);
            if (e != null && e.getEmail() != null && e.getEmail().equals(entrant.getEmail())) {
                entrantToMove = e;
                break;
            }
        }

        if (entrantToMove == null) {
            Toast.makeText(this, "Entrant not found in " + sourceListName, Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from source list and add to declined list
        sourceList.removeEntrant(entrantToMove);
        declinedList.addEntrant(entrantToMove);

        // Update the event object
        if (enrolledVisible.get()) {
            currentEvent.setAcceptedList(acceptedList);
        } else if (chosenVisible.get()) {
            currentEvent.setInviteList(inviteList);
        } else if (waitlistVisible.get()) {
            currentEvent.setWaitlist(waitlist);
        }
        currentEvent.setDeclinedList(declinedList);

        // Convert to Firestore format and build updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("declinedList", convertEntrantListToMap(declinedList));
        
        if (enrolledVisible.get()) {
            updates.put("acceptedList", convertEntrantListToMap(acceptedList));
        } else if (chosenVisible.get()) {
            updates.put("inviteList", convertEntrantListToMap(inviteList));
        } else if (waitlistVisible.get()) {
            updates.put("waitlist", convertEntrantListToMap(waitlist));
        }

        // Update Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(currentEvent.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Moved to cancelled list", Toast.LENGTH_SHORT).show();
                    // Reload data to reflect changes
                    loadData();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventManageActivity", "Failed to move entrant to cancelled list", e);
                    Toast.makeText(this, "Failed to move entrant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Converts EntrantList to a Map for Firestore storage.
     * This method mirrors the conversion logic from LotteryService.
     *
     * @param entrantList The entrant list to convert
     * @return A Map representation suitable for Firestore
     */
    private Map<String, Object> convertEntrantListToMap(EntrantList entrantList) {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> allEntrantsArray = new ArrayList<>();
        List<Map<String, Object>> waitListArray = new ArrayList<>();

        for (int i = 0; i < entrantList.size(); i++) {
            Entrant entrant = entrantList.getEntrant(i);
            if (entrant != null && entrant.getEmail() != null) {
                Map<String, Object> entrantMap = new HashMap<>();
                entrantMap.put("name", entrant.getName());
                entrantMap.put("email", entrant.getEmail());
                entrantMap.put("phoneNumber", entrant.getPhoneNumber());
                entrantMap.put("userType", entrant.getUserType());
                if (entrant.getPassword() != null) {
                    entrantMap.put("password", entrant.getPassword());
                }

                allEntrantsArray.add(entrantMap);
                waitListArray.add(entrantMap);
            }
        }

        map.put("allEntrants", allEntrantsArray);
        map.put("waitList", waitListArray);

        return map;
    }

    /**
     * Shows a dialog to compose and send a notification to a specific entrant.
     *
     * @param entrant The entrant to send the notification to
     */
    private void showSendNotificationDialog(Entrant entrant) {
        if (currentEvent == null || entrant == null || entrant.getEmail() == null) {
            Toast.makeText(this, "Unable to send notification", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog with an EditText for the message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to " + (entrant.getName() != null ? entrant.getName() : entrant.getEmail()));

        // Create EditText for message input
        final EditText messageEditText = new EditText(this);
        messageEditText.setHint("Enter notification message...");
        messageEditText.setMinLines(3);
        messageEditText.setMaxLines(5);
        messageEditText.setPadding(50, 20, 50, 20);
        builder.setView(messageEditText);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String message = messageEditText.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendNotificationToEntrant(entrant, message);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Sends a notification to a specific entrant.
     *
     * @param entrant The entrant to send the notification to
     * @param message The notification message
     */
    private void sendNotificationToEntrant(Entrant entrant, String message) {
        if (currentEvent == null || entrant == null || entrant.getEmail() == null) {
            Toast.makeText(this, "Unable to send notification", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerEmail = currentEvent.getOrganizer() != null 
                ? currentEvent.getOrganizer().getEmail() 
                : null;

        if (organizerEmail == null || organizerEmail.isEmpty()) {
            Toast.makeText(this, "Organizer information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = "Update about " + currentEvent.getEventName();
        String groupType = "Individual";
        
        Notification notification = new Notification(
                title,
                message,
                currentEvent.getId(),
                organizerEmail,
                currentEvent.getEventName(),
                groupType,
                1
        );

        notificationRepository.sendToUser(entrant.getEmail(), notification)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Notification sent to " + 
                            (entrant.getName() != null ? entrant.getName() : entrant.getEmail()), 
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("EventManageActivity", "Failed to send notification", e);
                    Toast.makeText(this, "Failed to send notification: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
}
