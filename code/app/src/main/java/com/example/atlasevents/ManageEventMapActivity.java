package com.example.atlasevents;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class ManageEventMapActivity extends AppCompatActivity implements OnMapReadyCallback{
    private GoogleMap entrantMap;
    private FusedLocationProviderClient fusedLocationClient;
    List<LatLng> entrantCoordList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file as the content view.
        setContentView(R.layout.manage_event_map);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get a handle to the fragment and register the callback.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Get a handle to the GoogleMap object and display marker.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        entrantMap = googleMap;
        //List<LatLng> entrantCoordList = new ArrayList<>();
        entrantCoordList.add(new LatLng(52, -113));
        entrantCoordList.add(new LatLng(52, -114));
        entrantCoordList.add(new LatLng(53, -113));
        entrantCoordList.add(new LatLng(53, -114));
        getLocation();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng coord: entrantCoordList) {
            entrantMap.addMarker(new MarkerOptions().position(coord).title("test"));
            builder.include(coord);
        }
        if (!entrantCoordList.isEmpty()) {
            LatLngBounds bounds = builder.build();
            entrantMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
        getLocation();

    }
       // test
        private void getLocation() {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;

        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double lat = location.getLatitude();
                    double longi = location.getLongitude();
                    LatLng testLocation = new LatLng(lat, longi);
                    entrantCoordList.add(testLocation);
                    entrantMap.addMarker(new MarkerOptions().position(testLocation).title("Coord Test"));
                }
            }
        });
        }
    // t
}
