package com.example.atlasevents.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.MapsInitializer;
import com.google.firebase.firestore.GeoPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight helper to warm up Google Maps and cache entrant coordinates
 * so the map screen can render faster.
 *
 * <p>Warm-up is triggered once per app process and runs on the main thread to comply
 * with Maps SDK requirements. Optionally caches coordinates and helps skip an
 * extra Firestore read if the data was just fetched on the parent screen.</p>
 */
public final class MapWarmUpManager {
    private static final String TAG = "MapWarmUpManager";
    private static final AtomicBoolean warming = new AtomicBoolean(false);
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Map<String, Map<String, GeoPoint>> entrantCache = new ConcurrentHashMap<>();

    private MapWarmUpManager() {
        // no-op
    }

    /**
     * Starts warming up Google Maps.
     *
     * <p>Safe to call multiple times; only the first call per process will execute.
     * Runs on the main thread because MapsInitializer enforces it.</p>
     *
     * @param context application or activity context used to initialize the Maps SDK
     */
    public static void warmUp(Context context) {
        if (context == null || initialized.get() || warming.get()) {
            return;
        }
        warming.set(true);

        // MapsInitializer requires the main thread.
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                MapsInitializer.initialize(context.getApplicationContext());
                initialized.set(true);
            } catch (Exception e) {
                Log.w(TAG, "Map warm-up failed", e);
            } finally {
                warming.set(false);
            }
        });
    }

    /**
     * Caches entrant coordinates for a given event.
     *
     * <p>Allows the map screen to avoid an extra fetch if opened shortly after the
     * data was retrieved on a parent screen.</p>
     *
     * @param eventId event identifier used as cache key
     * @param coords entrant coordinates keyed by entrant email
     */
    public static void cacheEntrantCoords(String eventId, Map<String, GeoPoint> coords) {
        if (TextUtils.isEmpty(eventId) || coords == null || coords.isEmpty()) {
            return;
        }
        entrantCache.put(eventId, new HashMap<>(coords));
    }

    /**
     * Retrieves and removes cached coordinates for an event, if present.
     *
     * @param eventId event identifier used as cache key
     * @return cached entrant coordinates or {@code null} if unavailable
     */
    public static Map<String, GeoPoint> consumeEntrantCoords(String eventId) {
        if (TextUtils.isEmpty(eventId)) {
            return null;
        }
        Map<String, GeoPoint> cached = entrantCache.remove(eventId);
        if (cached == null) {
            return null;
        }
        return Collections.unmodifiableMap(new HashMap<>(cached));
    }
}
