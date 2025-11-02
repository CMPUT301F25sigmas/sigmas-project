package com.example.atlasevents.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

/**
 * Minimal helpers to show in-app notifications.
 * Have to link with xml styles.
 */
public class NotificationHelper {
    /**
     * Shows a simple AlertDialog. Runs on UI thread.
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
     * Shows a Snackbar (non-blocking).
     * Provide a rootView (findViewById(android.R.id.content) from an Activity).
     */
    public static void showInAppSnackbar(Activity activity, String message) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            View root = activity.findViewById(android.R.id.content);
            if (root != null) Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        });
    }
}
