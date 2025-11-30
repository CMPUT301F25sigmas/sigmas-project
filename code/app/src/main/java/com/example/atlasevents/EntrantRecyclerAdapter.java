package com.example.atlasevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying a list of {@link Entrant} objects.
 * <p>
 * This adapter binds entrant data (name and email) to the views in
 * each item of a RecyclerView. It supports updating the entire list
 * dynamically through {@link #setEntrants(List)}.
 * </p>
 *
 * <p>Each item view is defined by {@code entrant_list_item.xml}.</p>
 *
 * @see Entrant
 * @see RecyclerView.Adapter
 */
public class EntrantRecyclerAdapter extends RecyclerView.Adapter<EntrantRecyclerAdapter.EntrantViewHolder> {

    /** List of entrants currently displayed in the RecyclerView. */
    private final List<Entrant> entrants;

    /** Click listener for entrant remove button. */
    private OnEntrantClickListener clickListener;

    /** Long press listener for sending notifications. */
    private OnEntrantLongClickListener longClickListener;

    /** Whether the remove button should be visible. */
    private boolean showRemoveButton = false;

    /**
     * Interface for handling entrant remove button clicks.
     */
    public interface OnEntrantClickListener {
        /**
         * Called when an entrant remove button is clicked.
         *
         * @param entrant The entrant that was clicked
         */
        void onEntrantClick(Entrant entrant);
    }

    /**
     * Interface for handling entrant long press events.
     */
    public interface OnEntrantLongClickListener {
        /**
         * Called when an entrant item is long pressed.
         *
         * @param entrant The entrant that was long pressed
         * @return true if the event was handled
         */
        boolean onEntrantLongClick(Entrant entrant);
    }

    /**
     * Constructs a new adapter for displaying entrants.
     *
     * @param initialEntrants The initial list of entrants to display, or {@code null} for an empty list.
     */
    public EntrantRecyclerAdapter(List<Entrant> initialEntrants) {
        this.entrants = initialEntrants != null ? new ArrayList<>(initialEntrants) : new ArrayList<>();
    }

    /**
     * Sets the click listener for entrant remove button.
     *
     * @param listener The click listener to set
     */
    public void setOnEntrantClickListener(OnEntrantClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Sets the long press listener for entrant items.
     *
     * @param listener The long press listener to set
     */
    public void setOnEntrantLongClickListener(OnEntrantLongClickListener listener) {
        this.longClickListener = listener;
    }

    /**
     * Sets whether the remove button should be visible.
     *
     * @param show Whether to show the remove button
     */
    public void setShowRemoveButton(boolean show) {
        this.showRemoveButton = show;
        notifyDataSetChanged();
    }

    /**
     * Replaces the current list of entrants with a new list and refreshes the RecyclerView.
     *
     * @param newEntrants The new list of entrants to display. If {@code null}, the list will be cleared.
     */
    public void setEntrants(List<Entrant> newEntrants) {
        this.entrants.clear();
        if (newEntrants != null) {
            this.entrants.addAll(newEntrants);
        }
        notifyDataSetChanged();
    }

    /**
     * Called when a new {@link EntrantViewHolder} is needed.
     * <p>
     * This method inflates the layout for each entrant item using
     * {@code entrant_list_item.xml}.
     * </p>
     *
     * @param parent   The parent view that will contain the new view holder.
     * @param viewType The type of the new view.
     * @return A new instance of {@link EntrantViewHolder}.
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entrant_list_item, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Binds an {@link Entrant} object to the corresponding {@link EntrantViewHolder}.
     * <p>
     * Sets the entrant’s name and email in the respective TextViews.
     * </p>
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the entrant in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        holder.nameTextView.setText(entrant.getName());
        holder.emailTextView.setText(entrant.getEmail());

        // Show or hide remove button based on flag
        holder.removeButton.setVisibility(showRemoveButton ? View.VISIBLE : View.GONE);

        // Set click listener on the remove button
        holder.removeButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onEntrantClick(entrant);
            }
        });

        // Set long press listener on the item view for sending notifications
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onEntrantLongClick(entrant);
            }
            return false;
        });
    }

    /**
     * Returns the number of entrants currently displayed.
     *
     * @return The total count of entrants.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder class representing a single entrant item in the RecyclerView.
     * <p>
     * Holds references to TextViews displaying the entrant's name and email.
     * </p>
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the entrant's name. */
        TextView nameTextView;

        /** TextView for displaying the entrant's email. */
        TextView emailTextView;

        /** ImageButton for removing the entrant. */
        ImageButton removeButton;

        /**
         * Creates a new ViewHolder instance and binds the view elements.
         *
         * @param itemView The inflated view for a single entrant item.
         */
        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrantName);
            emailTextView = itemView.findViewById(R.id.entrantEmail);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}

