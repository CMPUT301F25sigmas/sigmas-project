package com.example.atlasevents.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.example.atlasevents.R;

/**
 * Utility class for displaying user notifications in the application UI.
 * Provides methods for showing notifications as dialogs or snackbars on the main thread.
 *
 * <p>All methods automatically handle thread switching to the UI thread and null safety checks.</p>
 *
 * @see Activity
 * @see AlertDialog
 * @see Snackbar
 */
public class NotificationHelper {
    /**
     * Shows a simple AlertDialog with title and message on the UI thread.
     * This method is blocking and requires user interaction to dismiss.
     *
     * @param activity The Android activity context for displaying the dialog
     * @param title The title text for the dialog
     * @param message The message content for the dialog
     * @return void
     * @throws NullPointerException if activity is null (method returns early)
     * @see AlertDialog
     * @see Handler
     * @see Looper#getMainLooper()
     */
    public static void showInAppDialog(Activity activity, String title, String message) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (d, i) -> d.dismiss());
            builder.create().show();
        });
    }


    /**
     * Shows a non-blocking Snackbar with the specified message.
     * Snackbars automatically dismiss after a timeout and don't require user interaction.
     *
     * @param activity The Android activity context for displaying the snackbar
     * @param message The message content for the snackbar
     * @return void
     * @throws NullPointerException if activity is null (method returns early)
     * @see Snackbar
     * @see Handler
     * @see Looper#getMainLooper()
     */
    public static void showInAppSnackbar(Activity activity, String message) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            View root = activity.findViewById(android.R.id.content);
            if (root != null) Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        });
    }

    /**
     * Shows a lightweight toast (non-blocking, does not alter read status).
     * I lean on this when I want to alert the user without touching their notification state.
     */
    public static void showToast(Activity activity, String message) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Updates the launcher badge count 
     * Posts a silent notification with a badge number; cancels it when count is zero.
     * I keep it silent so only the badge changes and the user isn't pinged again.
     */
    public static void updateAppBadge(Context context, int count) {
        if (context == null) return;
        ensureBadgeChannel(context);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        int badgeId = 0xBADD1; // stable ID for badge updates

        if (count <= 0) {
            nm.cancel(badgeId);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "badge_channel")
                .setSmallIcon(R.drawable.notifications_icon)
                .setContentTitle("You have unread notifications")
                .setContentText(count > 99 ? "99+ new notifications" : count + " new notifications")
                .setNumber(count)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // The calling activity is responsible for requesting the permission.
                return;
            }
        }
        nm.notify(badgeId, builder.build());
    }

    /**
     * Ensures that the notification channel for badge updates exists.
     * This is required for Android 8.0 and higher.
     * <p>
     * The channel is created with low importance and no sound, making it suitable for
     * silent notifications that only update the app's badge count without disturbing the user.
     *
     * @param context The context used to access the system's NotificationManager.
     */
    private static void ensureBadgeChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                "badge_channel",
                "Badge Updates",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setShowBadge(true);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
