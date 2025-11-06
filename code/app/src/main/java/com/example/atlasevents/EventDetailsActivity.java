package com.example.atlasevents;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for displaying detailed information about an event.
 * <p>
 * This activity shows event details including the event name, organizer,
 * description, and a QR code representation of the event ID. It also provides
 * functionality for entrants to join or leave the event waitlist.
 * </p>
 * <p>
 * The event object is passed to this activity via an Intent extra using the
 * {@link #EventKey} identifier.
 * </p>
 *
 * @see Event
 * @see AppCompatActivity
 */
public class EventDetailsActivity extends AppCompatActivity {
    /**
     * Key used to pass the Event object through Intent extras.
     * This constant should be used when starting this activity to include
     * the event data in the intent.
     */
    public static String EventKey = "com.example.atlasevents.EVENT";

    /**
     * Generates a QR code bitmap from the given event ID.
     * <p>
     * Creates a 300x300 pixel QR code image using the ZXing library.
     * The QR code encodes the event ID as a string and renders it in
     * black and white.
     * </p>
     *
     * @param eventId The unique identifier of the event to encode in the QR code
     * @return A Bitmap containing the generated QR code image
     * @throws RuntimeException if QR code generation fails
     */
    private Bitmap generateQRCode(String eventId) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(eventId, BarcodeFormat.QR_CODE, 300, 300);
            Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565);
            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    if (bitMatrix.get(x, y)) {
                        bitmap.setPixel(x, y, Color.BLACK);
                    } else {
                        bitmap.setPixel(x, y, Color.WHITE);
                    }
                }
            }
            return bitmap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components and populates them with event data retrieved
     * from the Intent extras. Sets up click listeners for the join and leave
     * waitlist buttons (functionality to be implemented).
     * </p>
     * <p>
     * The event object is retrieved using {@link #EventKey} and its details are
     * displayed including:
     * </p>
     * <ul>
     *   <li>Event name</li>
     *   <li>Organizer name</li>
     *   <li>Event description</li>
     *   <li>QR code generated from the event ID</li>
     * </ul>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this Bundle contains the data it most
     *                           recently supplied. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_event_details);
        Event event = (Event) getIntent().getSerializableExtra(EventKey);
        TextView eventNameTextView = findViewById(R.id.eventName);
        TextView organizerNameTextView = findViewById(R.id.organizerName);
        TextView descriptionTextView = findViewById(R.id.eventDescription);
        TextView waitlistCountTextView = findViewById(R.id.waitlistCount);
        ImageView qrCodeImage = findViewById(R.id.eventImage);
        Button joinWaitlistButton = findViewById(R.id.joinWaitlistButton);
        Button leaveWaitlistButton = findViewById(R.id.leaveWaitlistButton);

        if (event != null) {
            eventNameTextView.setText(event.getEventName());
            organizerNameTextView.setText(event.getOrganizer().getName());
            descriptionTextView.setText(event.getDescription());
            qrCodeImage.setImageBitmap(generateQRCode(event.getId()));
        }
        // To be added later
        joinWaitlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        leaveWaitlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }
}