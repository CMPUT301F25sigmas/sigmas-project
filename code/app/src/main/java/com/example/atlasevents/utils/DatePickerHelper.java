package com.example.atlasevents.utils;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Date;

public class DatePickerHelper {
    private Date startDate;
    private Date endDate;
    private final Boolean pair;

    public interface DateSelectedCallback {
        void onSelected(Date start, Date end);
    }

    public DatePickerHelper(){
        this.pair = Boolean.FALSE;
    }
    public DatePickerHelper(Boolean pair) {
        this.pair = pair;
    }

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

    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }

    public void setStartDate(Date startDate) {this.startDate = startDate;}
    public void setEndDate(Date endDate) {this.endDate = endDate;}

    public String getStartDateFormatted() {
        if (startDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(startDate);
    }
    public String getEndDateFormatted() {
        if (endDate == null) {
            return null;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(endDate);
    }
}