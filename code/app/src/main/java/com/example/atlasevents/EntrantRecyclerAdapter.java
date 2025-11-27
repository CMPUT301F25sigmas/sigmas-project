package com.example.atlasevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    /**
     * Constructs a new adapter for displaying entrants.
     *
     * @param initialEntrants The initial list of entrants to display, or {@code null} for an empty list.
     */
    public EntrantRecyclerAdapter(List<Entrant> initialEntrants) {
        this.entrants = initialEntrants != null ? new ArrayList<>(initialEntrants) : new ArrayList<>();
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

        /**
         * Creates a new ViewHolder instance and binds the view elements.
         *
         * @param itemView The inflated view for a single entrant item.
         */
        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrantName);
            emailTextView = itemView.findViewById(R.id.entrantEmail);
        }
    }
}

