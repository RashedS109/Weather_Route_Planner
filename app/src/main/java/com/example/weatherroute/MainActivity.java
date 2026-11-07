package com.example.weatherroute;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.weatherroute.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseHelper db;
    private MapView map;

    // Firebase
    private FirebaseFirestore firestoreDB;
    private FirebaseAuth mAuth;
    private DatabaseReference realtimeDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MUST load osmdroid config before the map is created
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        // Tell OpenStreetMap who we are so they don't block our access
        Configuration.getInstance().setUserAgentValue("AdvancedProgrammingLab_WeatherApp_v1.0");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();

        // NOTE: If your counter gets stuck on "Loading...", paste your copied Realtime DB URL inside getInstance() below:
        // e.g., FirebaseDatabase.getInstance("https://your-url.firebasedatabase.app/").getReference("global_stats")
        realtimeDb = FirebaseDatabase.getInstance("https://weather-route-planner-75a41-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("global_stats");

        // Setup the Map
        map = binding.mapView;
        map.setTileSource(TileSourceFactory.OpenTopo);
        map.setMultiTouchControls(true);
        map.getController().setZoom(4.0);
        map.getController().setCenter(new GeoPoint(48.8566, 2.3522)); // Center on Europe

        // Initialize Database with Global Cities
        db = new DatabaseHelper(this);
        // Force-delete the old Bangladesh data so the new cities can load
        db.getWritableDatabase().execSQL("DELETE FROM cities");
        db.getWritableDatabase().execSQL("DELETE FROM roads");
        if (db.getAllCities().isEmpty()) {
            db.insertCity(1, "London", 51.5072, -0.1276);
            db.insertCity(2, "Paris", 48.8566, 2.3522);
            db.insertCity(3, "Oslo", 59.9139, 10.7522);
            db.insertCity(4, "Rome", 41.9028, 12.4964);
            db.insertCity(5, "Cairo", 30.0444, 31.2357);
        }

        if (db.getAllRoads().isEmpty()) {
            db.insertRoad(1, 2, 470);
            db.insertRoad(2, 1, 470);
            db.insertRoad(1, 3, 1800);
            db.insertRoad(3, 1, 1800);
            db.insertRoad(2, 3, 1600);
            db.insertRoad(3, 2, 1600);
            db.insertRoad(2, 4, 1400);
            db.insertRoad(4, 2, 1400);
            db.insertRoad(4, 5, 2100);
            db.insertRoad(5, 4, 2100);
        }

        // Setup UI Adapters
        binding.recyclerViewRoute.setLayoutManager(new LinearLayoutManager(this));

        List<City> cities = db.getAllCities();
        ArrayAdapter<City> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        binding.spinnerOrigin.setAdapter(adapter);
        binding.spinnerDestination.setAdapter(adapter);

        // Listen for LIVE updates from the cloud counter
        realtimeDb.child("total_routes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long count = snapshot.getValue(Long.class);
                if (count != null) {
                    binding.tvGlobalCounter.setText("Global Routes Calculated: " + count);
                } else {
                    binding.tvGlobalCounter.setText("Global Routes Calculated: 0");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error if needed
            }
        });

        // Background Executor for Multithreading
        ExecutorService executor = Executors.newSingleThreadExecutor();

        binding.btnCalculateRoute.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnCalculateRoute.setEnabled(false);

            City origin = (City) binding.spinnerOrigin.getSelectedItem();
            City destination = (City) binding.spinnerDestination.getSelectedItem();

            executor.execute(() -> {
                // Fetch all data from local SQLite database
                List<City> allCities = db.getAllCities();
                List<Road> allRoads = db.getAllRoads();

                // Run Dijkstra's Algorithm with Live Weather API penalties
                List<RouteStep> optimizedRoute = RouteOptimizer.calculateSafestRoute(origin, destination, allCities, allRoads);

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCalculateRoute.setEnabled(true);

                    RouteAdapter routeAdapter = new RouteAdapter(optimizedRoute);
                    binding.recyclerViewRoute.setAdapter(routeAdapter);

                    // Draw Route on OpenStreetMap
                    if (optimizedRoute.size() > 0 && !optimizedRoute.get(0).getSegment().equals("No safe route found")) {
                        map.getOverlays().clear(); // Clear previous search

                        GeoPoint startPoint = new GeoPoint(origin.getLat(), origin.getLon());
                        GeoPoint endPoint = new GeoPoint(destination.getLat(), destination.getLon());

                        // Add Start Marker
                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(startPoint);
                        startMarker.setTitle("Start: " + origin.getName());
                        map.getOverlays().add(startMarker);

                        // Add End Marker
                        Marker endMarker = new Marker(map);
                        endMarker.setPosition(endPoint);
                        endMarker.setTitle("End: " + destination.getName());
                        map.getOverlays().add(endMarker);

                        // Draw a line between the two cities
                        Polyline line = new Polyline();
                        line.addPoint(startPoint);
                        line.addPoint(endPoint);
                        line.setColor(Color.BLUE);
                        line.setWidth(8f);
                        map.getOverlays().add(line);

                        // Refresh map and zoom
                        map.invalidate();
                        map.getController().animateTo(startPoint);
                        map.getController().setZoom(5.0);

                        // Save to Firebase
                        saveRouteToCloud(origin.getName(), destination.getName(), optimizedRoute.size());
                    }
                });
            });
        });
    }

    private void saveRouteToCloud(String origin, String dest, int stepCount) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // Create a data package (HashMap) to send to Firestore
        Map<String, Object> routeData = new HashMap<>();
        routeData.put("origin", origin);
        routeData.put("destination", dest);
        routeData.put("totalSteps", stepCount);
        routeData.put("timestamp", System.currentTimeMillis());

        // Push to Cloud Firestore under the user's unique ID
        firestoreDB.collection("users").document(userId).collection("searchHistory")
                .add(routeData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Route saved to cloud!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Cloud save failed.", Toast.LENGTH_SHORT).show();
                });

        // Increment the global Realtime Database counter
        realtimeDb.child("total_routes").get().addOnSuccessListener(snapshot -> {
            long currentCount = 0;
            if (snapshot.exists() && snapshot.getValue() != null) {
                currentCount = (long) snapshot.getValue();
            }
            realtimeDb.child("total_routes").setValue(currentCount + 1);
        });
    }
}