package com.example.atlasevents.utils;

import android.app.TimePickerDialog;
import android.content.Context;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

/**
 * Utility class for a unified time picker.
 * <p>
 * Manages a time picker and provides direct
 * access to the selected time.
 * </p>
 */
public class TimePickerHelper {
    public Integer hour = null;
    public Integer minute = null;

    public interface TimeSelectedCallback {
        void onSelected(int hour, int minute);
    }

    /**
     * Displays the time picker dialog.
     * <p>
     * If hour and minute are already set, they will be selected in the picker.
     * </p>
     *
     * @param context the Context used to create the dialog
     * @param listener callback invoked when a time is selected
     */
    public void showPicker(Context context, TimeSelectedCallback listener) {
        Calendar cal = Calendar.getInstance();

        int pickerHour = (hour != null) ? hour : cal.get(Calendar.HOUR_OF_DAY);
        int pickerMinute = (minute != null) ? minute : cal.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                context,
                (v, h, m) -> {
                    this.hour = h;
                    this.minute = m;

                    if (listener != null) {
                        listener.onSelected(h, m);
                    }
                },
                pickerHour,
                pickerMinute,
                false
        );

        dialog.show();
    }

    /**
     * Gets the selected time formatted as "hh:mm AM/PM".
     *
     * @return the formatted time
     */
    public String getFormattedTime() {
        if (hour == null || minute == null) return null;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(cal.getTime()).toUpperCase(Locale.getDefault());
    }

    /**
     * Sets the time from a formatted string.
     * <p>
     * Parses a time string in "hh:mm AM/PM" format and sets the hour and minute fields.
     * </p>
     *
     * @param timeString the time string to parse
     */
    public void setTimeFromString(String timeString) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        try {
            Date date = sdf.parse(timeString);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                this.hour = cal.get(Calendar.HOUR_OF_DAY);
                this.minute = cal.get(Calendar.MINUTE);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            this.hour = null;
            this.minute = null;
        }
    }
}