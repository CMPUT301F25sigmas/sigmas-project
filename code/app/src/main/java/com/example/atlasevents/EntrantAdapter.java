package com.example.atlasevents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class EntrantAdapter extends ArrayAdapter<Entrant> {

    public EntrantAdapter(Context context, ArrayList<Entrant> entrants) {
        super(context, 0, entrants);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.entrant_list_item, parent, false);
        } else {
            view = convertView;
        }

        Entrant entrant = getItem(position);

        TextView nameTextView = view.findViewById(R.id.entrantName);
        TextView emailTextView = view.findViewById(R.id.entrantEmail);

        nameTextView.setText(entrant.getName());
        emailTextView.setText(entrant.getEmail());

        return view;
    }
}