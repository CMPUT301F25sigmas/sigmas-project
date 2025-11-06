package com.example.atlasevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EntrantRecyclerAdapter extends RecyclerView.Adapter<EntrantRecyclerAdapter.EntrantViewHolder> {

    private final List<Entrant> entrants;

    public EntrantRecyclerAdapter(List<Entrant> initialEntrants) {
        this.entrants = initialEntrants != null ? new ArrayList<>(initialEntrants) : new ArrayList<>();
    }

    public void setEntrants(List<Entrant> newEntrants) {
        this.entrants.clear();
        if (newEntrants != null) {
            this.entrants.addAll(newEntrants);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrant_list_item, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        holder.nameTextView.setText(entrant.getName());
        holder.emailTextView.setText(entrant.getEmail());
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView emailTextView;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrantName);
            emailTextView = itemView.findViewById(R.id.entrantEmail);
        }
    }
}
