package com.example.atlasevents.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

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
}
