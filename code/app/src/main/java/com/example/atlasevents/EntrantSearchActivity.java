package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Entrant search screen that queries Firestore as the user types, with a debounce
 * and two-keystroke threshold to reduce network calls. Supports searching by event
 * name or tags through the precomputed searchKeywords field.
 */
public class EntrantSearchActivity extends EntrantBase {

    private static final long SEARCH_DEBOUNCE_MS = 350L;

    private SearchView searchView;
    private RecyclerView eventsRecyclerView;
    private LinearLayout emptyState;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String lastRequestedQuery = "";
    private int lastRequestedLength = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.search_page);
        setActiveNavItem(R.id.search_icon_card);

        searchView = findViewById(R.id.search_view);
        eventsRecyclerView = findViewById(R.id.events_recycler_view);
        emptyState = findViewById(R.id.empty_view_entrant);

        eventRepository = new EventRepository();
        adapter = new EventCardAdapter(this::openEventDetails);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(adapter);

        toggleEmptyState(true);

        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                scheduleSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleSearch(newText);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lastRequestedQuery.length() >= 2) {
            searchEvents(lastRequestedQuery);
        }
    }

    private void scheduleSearch(String rawQuery) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> triggerSearch(rawQuery);
        searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
    }

    /**
     * Enforces a two-keystroke threshold between Firestore calls while still debouncing typing.
     */
    private void triggerSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            adapter.setEvents(new ArrayList<>());
            lastRequestedQuery = "";
            lastRequestedLength = 0;
            toggleEmptyState(true);
            return;
        }

        int currentLength = query.length();
        if (currentLength < 2) {
            return;
        }

        if (Math.abs(currentLength - lastRequestedLength) < 2 && !query.equals(lastRequestedQuery)) {
            return;
        }

        if (query.equals(lastRequestedQuery)) {
            return;
        }

        lastRequestedQuery = query;
        lastRequestedLength = currentLength;
        searchEvents(query);
    }

    private void searchEvents(String query) {
        eventRepository.searchEventsByKeyword(query, new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(ArrayList<Event> events) {
                adapter.setEvents(events);
                toggleEmptyState(events.isEmpty());
            }

            @Override
            public void onFailure(Exception e) {
                adapter.setEvents(new ArrayList<>());
                toggleEmptyState(true);
            }
        });
    }

    private void toggleEmptyState(boolean showEmpty) {
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        eventsRecyclerView.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
