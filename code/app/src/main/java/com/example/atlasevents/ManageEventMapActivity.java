package com.example.atlasevents;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.atlasevents.data.EventRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapView;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;

/**
 * Displays entrant join locations for an event on Google Maps.
 * Opens in a small preview layout by default with a button to launch full-screen.
 */
public class ManageEventMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String EXTRA_EVENT_ID = "EVENT_ID";
    private static final String EXTRA_FULL_SCREEN = "FULL_SCREEN";
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap entrantMap;
    private EventRepository eventRepository;
    private String eventId;
    private MapView mapView;
    private boolean isFullScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventRepository = new EventRepository();
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        isFullScreen = getIntent().getBooleanExtra(EXTRA_FULL_SCREEN, false);

        if (isFullScreen) {
            setContentView(R.layout.manage_event_map);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } else {
            setContentView(R.layout.map_view);
            mapView = findViewById(R.id.mapView);
            Button fullScreenButton = findViewById(R.id.full_screen_button);
            ImageButton backButton = findViewById(R.id.backButton);

            Bundle mapViewBundle = null;
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
            }
            mapView.onCreate(mapViewBundle);
            mapView.getMapAsync(this);

            backButton.setOnClickListener(v -> finish());
            fullScreenButton.setOnClickListener(v -> openFullScreenMap());
        }
    }

    private void openFullScreenMap() {
        if (eventId == null) {
            Toast.makeText(this, "No event to display", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ManageEventMapActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_FULL_SCREEN, true);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
            }
            mapView.onSaveInstanceState(mapViewBundle);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    private void mapMarker(String id) {
        eventRepository.getEventById(id, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                entrantMap.clear();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasPins = false;
                Map<String, GeoPoint> locations = event.getEntrantCoords();
                if (locations != null && !locations.isEmpty()) {
                    for (Map.Entry<String, GeoPoint> entry : locations.entrySet()) {
                        String email = entry.getKey();
                        GeoPoint coord = entry.getValue();
                        if (coord != null) {
                            LatLng point = new LatLng(coord.getLatitude(), coord.getLongitude());
                            entrantMap.addMarker(new MarkerOptions().position(point).title(email));
                            builder.include(point);
                            hasPins = true;
                        }
                    }
                }
                if (hasPins) {
                    LatLngBounds bounds = builder.build();
                    entrantMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                } else {
                    Toast.makeText(ManageEventMapActivity.this, "No entrant locations yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ManageEventMapActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        entrantMap = googleMap;

        if (eventId == null) {
            Toast.makeText(this, "No event to display", Toast.LENGTH_SHORT).show();
        } else {
            mapMarker(eventId);
        }
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (entrantMap == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        entrantMap.setMyLocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission needed to show your position", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
