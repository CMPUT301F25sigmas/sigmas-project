package com.example.atlasevents.utils;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Date;

/**
 * Utility class for a unified date picker.
 * <p>
 * Manages a material date picker and provides direct
 * access to the dates picked as well as formatted dates
 * </p>
 */
public class DatePickerHelper {
    private Date startDate;
    private Date endDate;
    private final Boolean pair;

    public interface DateSelectedCallback {
        void onSelected(Date start, Date end);
    }

    /**
     * Constructs a DatePickerHelper for single date selection.
     */
    public DatePickerHelper(){
        this.pair = Boolean.FALSE;
    }

    /**
     * Constructs a DatePickerHelper with configurable date selection.
     *
     * @param pair true for date range selection, false for single date selection
     */
    public DatePickerHelper(Boolean pair) {
        this.pair = pair;
    }

    /**
     * Displays the date picker dialog.
     * <p>
     * Shows either a single date picker or date range picker based on the pair setting.
     * If startDate or endDate are already set, they will be selected in the picker.
     * </p>
     *
     * @param fragmentManager the FragmentManager used to show the dialog
     * @param listener callback invoked when dates are selected
     */
    public void showPicker(FragmentManager fragmentManager, DateSelectedCallback listener) {
        if (!pair) {
            MaterialDatePicker.Builder<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Pick a date");

            if (startDate != null) {
                datePicker.setSelection(startDate.getTime());
            }

            MaterialDatePicker<Long> picker = datePicker.build();
            picker.addOnPositiveButtonClickListener(dateRange -> {
                startDate = new Date(dateRange);
                endDate = null;
                listener.onSelected(startDate, null);
            });

            picker.show(fragmentManager, "Date Picker");
        } else {
            MaterialDatePicker.Builder<Pair<Long, Long>> datePicker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Pick a date range");

            if (startDate != null && endDate != null) {
                datePicker.setSelection(new Pair<>(startDate.getTime(), endDate.getTime()));
            }

            MaterialDatePicker<Pair<Long, Long>> picker = datePicker.build();
            picker.addOnPositiveButtonClickListener(dateRange -> {
                startDate = new Date(dateRange.first);
                endDate = new Date(dateRange.second);
                listener.onSelected(startDate, endDate);
            });

            picker.show(fragmentManager, "Date Picker");
        }
    }

    /**
     * Gets the selected start date.
     *
     * @return the start date
     */
    public Date getStartDate() { return startDate; }

    /**
     * Gets the selected end date.
     *
     * @return the end date
     */
    public Date getEndDate() { return endDate; }

    /**
     * Sets the start date.
     *
     * @param startDate the start date to set
     */
    public void setStartDate(Date startDate) {this.startDate = startDate;}

    /**
     * Sets the end date.
     *
     * @param endDate the end date to set
     */
    public void setEndDate(Date endDate) {this.endDate = endDate;}

    /**
     * Gets the start date formatted as "MMM dd, yyyy".
     *
     * @return the formatted start date
     */
    public String getStartDateFormatted() {
        if (startDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(startDate);
    }

    /**
     * Gets the end date formatted as "MMM dd, yyyy"
     *
     * @return the formatted end date
     */
    public String getEndDateFormatted() {
        if (endDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(endDate);
    }
}