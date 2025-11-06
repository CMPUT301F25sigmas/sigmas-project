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

    /**
     * Get a View that displays the data at the specified position in the data set.
     *
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@link #getViewTypeCount()} and
     *        {@link #getItemViewType(int)}).
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
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