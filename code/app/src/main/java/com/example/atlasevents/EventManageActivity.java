package com.example.atlasevents;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;

public class EventManageActivity extends AppCompatActivity {

    public static final String EventKey = "com.example.atlasevents.EVENT";

    private EventRepository eventRepository;

    TextView eventNameTextView;
    TextView waitlistCountTextView;
    TextView dateTextView;
    TextView locationTextView;
    ImageView eventImageView;

    RecyclerView entrantsRecyclerView;
    EntrantRecyclerAdapter entrantAdapter;
    CardView waitingListCard;
    ArrayList<Entrant> entrantList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventRepository = new EventRepository();

        eventNameTextView = findViewById(R.id.eventTitle);
        waitlistCountTextView = findViewById(R.id.waitingListCount);
        dateTextView = findViewById(R.id.eventDate);
        locationTextView = findViewById(R.id.eventLocation);
        eventImageView = findViewById(R.id.eventPoster);
        waitingListCard = findViewById(R.id.waitingListViewCard);
        entrantsRecyclerView = findViewById(R.id.entrantsRecyclerView);

        entrantList = new ArrayList<>();
        entrantAdapter = new EntrantRecyclerAdapter(entrantList);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        entrantsRecyclerView.setAdapter(entrantAdapter);

//        backArrow = findViewById(R.id.back_arrow);

        loadData();
    }

    private void loadData(){
        eventRepository.getEventById(getIntent().getSerializableExtra(EventKey).toString(), new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                eventNameTextView.setText(event.getEventName());
                waitlistCountTextView.setText(String.valueOf(event.getWaitlist() != null ? event.getWaitlist().size() : 0));
                dateTextView.setText(event.getDate());
                locationTextView.setText(event.getAddress());
                if(!event.getImageUrl().isEmpty()){
                    Glide.with(EventManageActivity.this).load(event.getImageUrl()).into(eventImageView);
                } else {
                    eventImageView.setImageResource(R.drawable.poster);
                }
                if (event.getWaitlist() != null && event.getWaitlist().getWaitList() != null && !event.getWaitlist().getWaitList().isEmpty()) {
                    entrantAdapter.setEntrants(event.getWaitlist().getWaitList());
                    waitingListCard.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EventDetailsActivity", "Failed to fetch event", e);
                Toast.makeText(EventManageActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
