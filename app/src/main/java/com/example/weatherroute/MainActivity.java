package com.example.weatherroute;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.weatherroute.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Database
        db = new DatabaseHelper(this);
        if (db.getAllCities().isEmpty()) {
            db.insertCity(1, "Dhaka", 23.8103, 90.4125);
            db.insertCity(2, "Khulna", 22.8456, 89.5403);
            db.insertCity(3, "Rajshahi", 24.3636, 88.6241);
            db.insertCity(4, "Chittagong", 22.3569, 91.7832);
        }

        // Setup Spinners with Data from SQLite
        List<City> cities = db.getAllCities();
        ArrayAdapter<City> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        binding.spinnerOrigin.setAdapter(adapter);
        binding.spinnerDestination.setAdapter(adapter);

        // Setup RecyclerView
        binding.recyclerViewRoute.setLayoutManager(new LinearLayoutManager(this));

        // Setup Button Click Listener (We will put actual Dijkstra logic here later)
        binding.btnCalculateRoute.setOnClickListener(v -> {
            // Temporary dummy data to test the UI
            List<RouteStep> dummySteps = new ArrayList<>();
            dummySteps.add(new RouteStep("Dhaka → Khulna", "Distance: 200 km", "Weather: Clear"));
            dummySteps.add(new RouteStep("Khulna → Chittagong", "Distance: 350 km", "Weather: Heavy Rain"));

            RouteAdapter routeAdapter = new RouteAdapter(dummySteps);
            binding.recyclerViewRoute.setAdapter(routeAdapter);
        });
    }
}