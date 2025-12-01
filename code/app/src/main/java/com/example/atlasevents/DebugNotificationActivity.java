package com.example.atlasevents;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.atlasevents.data.NotificationRepository;
import com.example.atlasevents.data.model.Notification;
//import com.example.atlasevents.Session;
//import com.example.atlasevents.R;
import java.util.Arrays;
import java.util.List;

public class DebugNotificationActivity extends AppCompatActivity {
    private static final String TAG = "DebugNotifDebugAct";

    private EditText etTargetEmail;
    private EditText etCustomMessage;
    private TextView tvStatus;
    private NotificationRepository notifRepo;
    private Session session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_notifications);

        notifRepo = new NotificationRepository();
        session = new Session(this);

        etTargetEmail = findViewById(R.id.et_target_email);
        etCustomMessage = findViewById(R.id.et_custom_message);
        tvStatus = findViewById(R.id.tv_debug_status);

        Button btnSelf = findViewById(R.id.btn_send_to_self);
        Button btnToEmail = findViewById(R.id.btn_send_to_email);
        Button btnBatch = findViewById(R.id.btn_send_batch_example);

        btnSelf.setOnClickListener(v -> sendToSelf());
        btnToEmail.setOnClickListener(v -> sendToEmail());
        btnBatch.setOnClickListener(v -> sendBatchExample());
    }

    private void sendToSelf() {
        String myEmail = session.getUserEmail();
        if (myEmail == null) {
            Toast.makeText(this, "No logged-in email in session", Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = etCustomMessage.getText().toString();
        if (msg.isEmpty()) msg = "Test notification (to self)";

        Notification n = new Notification("Debug: You were selected", msg, "debug_event_001", getOrganizerEmailForDebug(), "Debug Event", "Debug Group");
        tvStatus.setText("Status: sending to " + myEmail);
        notifRepo.sendToUser(myEmail, n)
                .addOnSuccessListener(aVoid -> {
                    tvStatus.setText("Status: sent to " + myEmail);
                    Log.d(TAG, "Sent debug notification to self: " + myEmail);
                    Toast.makeText(this, "Sent to " + myEmail, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Status: failed: " + e.getMessage());
                    Log.e(TAG, "Failed to send debug notif", e);
                    Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendToEmail() {
        String target = etTargetEmail.getText().toString().trim();
        if (target.isEmpty()) {
            Toast.makeText(this, "Enter a target email or use 'Send to Me'", Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = etCustomMessage.getText().toString();
        if (msg.isEmpty()) msg = "Test notification (manual target)";

        Notification n = new Notification("Debug: Organizer message", msg, "debug_event_002", getOrganizerEmailForDebug(), "Debug Event", "Debug Group");
        tvStatus.setText("Status: sending to " + target);
        notifRepo.sendToUser(target, n)
                .addOnSuccessListener(aVoid -> {
                    tvStatus.setText("Status: sent to " + target);
                    Log.d(TAG, "Sent debug notification to: " + target);
                    Toast.makeText(this, "Sent to " + target, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Status: failed: " + e.getMessage());
                    Log.e(TAG, "Failed to send debug notif to " + target, e);
                    Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendBatchExample() {
        // change these test emails to match your test accounts
        List<String> emails = Arrays.asList(
                "JohnDoe@gmail.com",
                "bob2@example1.com",
                "tt@t.com"
        );
        String msg = etCustomMessage.getText().toString();
        if (msg.isEmpty()) msg = "Batch test notification";

        Notification n = new Notification("Batch test", msg, "debug_event_batch", getOrganizerEmailForDebug(), "Debug Event", "Debug Group");

        tvStatus.setText("Status: sending batch to " + emails.size() + " users");
        notifRepo.sendToUsers(emails, n)
                .addOnSuccessListener(tasksList -> {
                    tvStatus.setText("Status: batch send scheduled");
                    Toast.makeText(this, "Batch send triggered", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Batch send triggered to " + emails.size());
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Status: failed: " + e.getMessage());
                    Log.e(TAG, "Batch send failed", e);
                    Toast.makeText(this, "Batch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // For debug we return the currently logged-in user as organizer (or a fixed debug organizer)
    private String getOrganizerEmailForDebug() {
        String me = session.getUserEmail();
        return me != null ? me : "org-debug@example.com";
    }
}
