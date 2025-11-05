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

public class EventDetailsActivity extends AppCompatActivity {
    public static String EventKey = "com.example.atlasevents.EVENT";
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
