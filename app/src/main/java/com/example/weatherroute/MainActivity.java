package com.example.weatherroute;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.weatherroute.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseHelper db;
    private FirebaseFirestore firestoreDB;
    private FirebaseAuth mAuth;
    private DatabaseReference realtimeDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Database
        db = new DatabaseHelper(this);
        firestoreDB = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        realtimeDb = FirebaseDatabase.getInstance("https://weather-route-planner-75a41-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("global_stats");

        // 1. Listen for LIVE updates from the cloud
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


        // 1. Populate cities if empty
        if (db.getAllCities().isEmpty()) {
            db.insertCity(1, "Dhaka", 23.8103, 90.4125);
            db.insertCity(2, "Khulna", 22.8456, 89.5403);
            db.insertCity(3, "Rajshahi", 24.3636, 88.6241);
            db.insertCity(4, "Chittagong", 22.3569, 91.7832);
        }

        // 2. Populate roads if empty (This fixes the bug!)
        if (db.getAllRoads().isEmpty()) {
            db.insertRoad(1, 2, 200); // Dhaka to Khulna
            db.insertRoad(2, 1, 200);
            db.insertRoad(1, 3, 250); // Dhaka to Rajshahi
            db.insertRoad(3, 1, 250);
            db.insertRoad(1, 4, 250); // Dhaka to Chittagong
            db.insertRoad(4, 1, 250);
            db.insertRoad(2, 3, 280); // Khulna to Rajshahi
            db.insertRoad(3, 2, 280);
        }

        // Setup Spinners with Data from SQLite
        List<City> cities = db.getAllCities();
        ArrayAdapter<City> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        binding.spinnerOrigin.setAdapter(adapter);
        binding.spinnerDestination.setAdapter(adapter);

        // Setup RecyclerView
        binding.recyclerViewRoute.setLayoutManager(new LinearLayoutManager(this));

        // Setup Button Click Listener (We will put actual Dijkstra logic here later)
        // Create an Executor for background thread management (Week 4 Requirement)
        ExecutorService executor = Executors.newSingleThreadExecutor();

        binding.btnCalculateRoute.setOnClickListener(v -> {
            // 1. Show loading spinner on the UI thread
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnCalculateRoute.setEnabled(false);

            // 2. Get selected cities
            City origin = (City) binding.spinnerOrigin.getSelectedItem();
            City destination = (City) binding.spinnerDestination.getSelectedItem();

            // 3. Execute network operations in a background thread
            // 3. Execute network and Dijkstra operations in a background thread
            executor.execute(() -> {
                // Fetch all data from local SQLite database
                List<City> allCities = db.getAllCities();
                List<Road> allRoads = db.getAllRoads();

                // Run Dijkstra's Algorithm
                List<RouteStep> optimizedRoute = RouteOptimizer.calculateSafestRoute(origin, destination, allCities, allRoads);

                // 4. Post the results back to the Main UI Thread
                // 4. Post the results back to the Main UI Thread
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCalculateRoute.setEnabled(true);

                    RouteAdapter routeAdapter = new RouteAdapter(optimizedRoute);
                    binding.recyclerViewRoute.setAdapter(routeAdapter);

                    // NEW: Save the search to Cloud Firestore
                    if (optimizedRoute.size() > 0 && !optimizedRoute.get(0).getSegment().equals("No safe route found")) {
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