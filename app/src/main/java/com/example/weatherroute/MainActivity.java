package com.example.weatherroute;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.weatherroute.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    // Android Core Components
    private AirplaneModeReceiver airplaneModeReceiver = new AirplaneModeReceiver();
    private FusedLocationProviderClient fusedLocationClient;

    // Dynamic Graph Tracker for Bellman-Ford (City ID -> Penalty Multiplier)
    private Map<Integer, Double> activeStorms = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue("AdvancedProgrammingLab_WeatherApp_v1.0");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();

        // PASTE YOUR REALTIME DATABASE URL HERE:
        realtimeDb = FirebaseDatabase.getInstance("https://weather-route-planner-75a41-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("global_stats");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        map = binding.mapView;
        map.setTileSource(TileSourceFactory.OpenTopo);
        map.setMultiTouchControls(true);
        map.getController().setZoom(7.0);
        map.getController().setCenter(new GeoPoint(23.8103, 90.4125)); // Center on Dhaka

        db = new DatabaseHelper(this);
        db.getWritableDatabase().execSQL("DELETE FROM cities");
        db.getWritableDatabase().execSQL("DELETE FROM roads");

        if (db.getAllCities().isEmpty()) {
            db.insertCity(1, "Khulna", 22.8456, 89.5403);
            db.insertCity(2, "Dhaka", 23.8103, 90.4125);
            db.insertCity(3, "Rajshahi", 24.3745, 88.6042);
            db.insertCity(4, "Chittagong", 22.3569, 91.7832);
            db.insertCity(5, "Sylhet", 24.8949, 91.8687);
        }

        if (db.getAllRoads().isEmpty()) {
            // Main Highways through Dhaka
            db.insertRoad(1, 2, 270);
            db.insertRoad(2, 1, 270);

            db.insertRoad(3, 2, 250);
            db.insertRoad(2, 3, 250);

            db.insertRoad(1, 3, 280);
            db.insertRoad(3, 1, 280);

            db.insertRoad(2, 4, 250);
            db.insertRoad(4, 2, 250);

            db.insertRoad(2, 5, 240);
            db.insertRoad(5, 2, 240);

            // Eastern Highway
            db.insertRoad(4, 5, 340);
            db.insertRoad(5, 4, 340);

            // Coastal/Ferry Bypass (avoids Dhaka entirely)
            db.insertRoad(1, 4, 350);
            db.insertRoad(4, 1, 350);
        }

        binding.recyclerViewRoute.setLayoutManager(new LinearLayoutManager(this));

        List<City> cities = db.getAllCities();
        ArrayAdapter<City> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        binding.spinnerOrigin.setAdapter(adapter);
        binding.spinnerDestination.setAdapter(adapter);

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
            public void onCancelled(DatabaseError error) {}
        });

        binding.btnDetectLocation.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndSnapToCity();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
            }
        });

        binding.btnShareContact.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                accessContactsProvider();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 100);
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();

        binding.btnCalculateRoute.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnCalculateRoute.setEnabled(false);

            City origin = (City) binding.spinnerOrigin.getSelectedItem();
            City destination = (City) binding.spinnerDestination.getSelectedItem();

            if (origin.getId() == destination.getId()) {
                Toast.makeText(MainActivity.this, "Origin and Destination cannot be the same!", Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
                binding.btnCalculateRoute.setEnabled(true);
                return;
            }

            RouteOptimizer.RoutingMode selectedMode = RouteOptimizer.RoutingMode.BALANCED;
            int selectedId = binding.radioGroupMode.getCheckedRadioButtonId();
            if (selectedId == R.id.radioSafest) {
                selectedMode = RouteOptimizer.RoutingMode.SAFEST_FIRST;
            } else if (selectedId == R.id.radioShortest) {
                selectedMode = RouteOptimizer.RoutingMode.SHORTEST_FIRST;
            }
            final RouteOptimizer.RoutingMode finalMode = selectedMode;

            executor.execute(() -> {
                List<City> allCities = db.getAllCities();
                List<Road> allRoads = db.getAllRoads();

                runOnUiThread(() -> map.getOverlays().clear());

                List<RouteStep> optimizedRoute = RouteOptimizer.calculateOptimalRoute(origin, destination, allCities, allRoads, finalMode, (fromCity, toCity) -> {
                    runOnUiThread(() -> {
                        GeoPoint scanStart = new GeoPoint(fromCity.getLat(), fromCity.getLon());
                        GeoPoint scanEnd = new GeoPoint(toCity.getLat(), toCity.getLon());

                        Polyline evalLine = new Polyline();
                        evalLine.addPoint(scanStart);
                        evalLine.addPoint(scanEnd);
                        evalLine.setColor(Color.parseColor("#99FFEB3B"));
                        evalLine.setWidth(6f);
                        map.getOverlays().add(evalLine);
                        map.invalidate();
                    });
                });

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnCalculateRoute.setEnabled(true);

                    RouteAdapter routeAdapter = new RouteAdapter(optimizedRoute);
                    binding.recyclerViewRoute.setAdapter(routeAdapter);

                    if (optimizedRoute.size() > 0 && !optimizedRoute.get(0).getSegment().equals("No safe route found")) {
                        GeoPoint startPoint = new GeoPoint(origin.getLat(), origin.getLon());
                        GeoPoint endPoint = new GeoPoint(destination.getLat(), destination.getLon());

                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(startPoint);
                        startMarker.setTitle("Start: " + origin.getName());
                        map.getOverlays().add(startMarker);

                        Marker endMarker = new Marker(map);
                        endMarker.setPosition(endPoint);
                        endMarker.setTitle("End: " + destination.getName());
                        map.getOverlays().add(endMarker);

                        Polyline finalLine = new Polyline();
                        finalLine.addPoint(startPoint);
                        for (RouteStep step : optimizedRoute) {
                            String[] cityNames = step.getSegment().split(" -> ");
                            if (cityNames.length == 2) {
                                City stepCity = getCityByName(allCities, cityNames[1]);
                                if (stepCity != null) {
                                    finalLine.addPoint(new GeoPoint(stepCity.getLat(), stepCity.getLon()));
                                }
                            }
                        }

                        finalLine.setColor(Color.BLUE);
                        finalLine.setWidth(10f);
                        map.getOverlays().add(finalLine);

                        map.invalidate();
                        map.getController().animateTo(startPoint);
                        map.getController().setZoom(7.0);

                        saveRouteToCloud(origin.getName(), destination.getName(), optimizedRoute.size());
                        startService(new Intent(MainActivity.this, NavigationService.class));
                    } else {
                        Toast.makeText(MainActivity.this, "No route found between these cities.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        binding.btnSimulateStorm.setOnClickListener(v -> {
            City origin = (City) binding.spinnerOrigin.getSelectedItem();
            City destination = (City) binding.spinnerDestination.getSelectedItem();

            if (origin.getId() == destination.getId()) return;

            binding.progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "⚠️ STORM DETECTED IN DHAKA! Recalculating with Bellman-Ford...", Toast.LENGTH_LONG).show();

            executor.execute(() -> {
                List<City> allCities = db.getAllCities();
                List<Road> allRoads = db.getAllRoads();

                activeStorms.clear();
                activeStorms.put(2, 9999.0); // 2 = Dhaka

                List<RouteStep> stormRoute = RouteOptimizer.calculateRouteBellmanFord(origin, destination, allCities, allRoads, activeStorms);

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);

                    RouteAdapter routeAdapter = new RouteAdapter(stormRoute);
                    binding.recyclerViewRoute.setAdapter(routeAdapter);

                    if (stormRoute.size() > 0 && !stormRoute.get(0).getSegment().equals("No safe route found")) {
                        map.getOverlays().clear();

                        GeoPoint startPoint = new GeoPoint(origin.getLat(), origin.getLon());
                        GeoPoint endPoint = new GeoPoint(destination.getLat(), destination.getLon());

                        Marker startMarker = new Marker(map);
                        startMarker.setPosition(startPoint);
                        startMarker.setTitle("Start: " + origin.getName());
                        map.getOverlays().add(startMarker);

                        Marker endMarker = new Marker(map);
                        endMarker.setPosition(endPoint);
                        endMarker.setTitle("End: " + destination.getName());
                        map.getOverlays().add(endMarker);

                        Polyline emergencyLine = new Polyline();
                        emergencyLine.addPoint(startPoint);
                        for (RouteStep step : stormRoute) {
                            String[] cityNames = step.getSegment().split(" -> ");
                            if (cityNames.length == 2) {
                                City stepCity = getCityByName(allCities, cityNames[1]);
                                if (stepCity != null) {
                                    emergencyLine.addPoint(new GeoPoint(stepCity.getLat(), stepCity.getLon()));
                                }
                            }
                        }

                        emergencyLine.setColor(Color.RED);
                        emergencyLine.setWidth(10f);
                        map.getOverlays().add(emergencyLine);

                        map.invalidate();
                    }
                });
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(airplaneModeReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(airplaneModeReceiver);
    }

    private void saveRouteToCloud(String origin, String dest, int stepCount) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> routeData = new HashMap<>();
        routeData.put("origin", origin);
        routeData.put("destination", dest);
        routeData.put("totalSteps", stepCount);
        routeData.put("timestamp", System.currentTimeMillis());

        firestoreDB.collection("users").document(userId).collection("searchHistory")
                .add(routeData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Route saved to cloud!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Cloud save failed.", Toast.LENGTH_SHORT).show();
                });

        realtimeDb.child("total_routes").get().addOnSuccessListener(snapshot -> {
            long currentCount = 0;
            if (snapshot.exists() && snapshot.getValue() != null) {
                currentCount = (long) snapshot.getValue();
            }
            realtimeDb.child("total_routes").setValue(currentCount + 1);
        });
    }

    @SuppressLint("Range")
    private void accessContactsProvider() {
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            Toast.makeText(this, "Route shared with: " + contactName, Toast.LENGTH_LONG).show();
            cursor.close();
        } else {
            Toast.makeText(this, "No contacts found on device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocationAndSnapToCity() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double userLat = location.getLatitude();
                double userLon = location.getLongitude();

                List<City> cities = db.getAllCities();
                City nearestCity = null;
                double minDistance = Double.MAX_VALUE;

                for (City city : cities) {
                    double dist = calculateHaversineDistance(userLat, userLon, city.getLat(), city.getLon());
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearestCity = city;
                    }
                }

                if (nearestCity != null) {
                    ArrayAdapter<City> adapter = (ArrayAdapter<City>) binding.spinnerOrigin.getAdapter();
                    int spinnerPosition = adapter.getPosition(nearestCity);
                    binding.spinnerOrigin.setSelection(spinnerPosition);

                    Toast.makeText(this, "Snapped to nearest city: " + nearestCity.getName(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Unable to detect GPS. Ensure location is turned on.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private City getCityByName(List<City> cities, String name) {
        for (City c : cities) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }
}