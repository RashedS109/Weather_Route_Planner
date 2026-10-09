package com.example.weatherroute;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weatherroute.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Database logic MUST be inside the onCreate method
        DatabaseHelper db = new DatabaseHelper(this);

        // Populate local database if empty
        if (db.getAllCities().isEmpty()) {
            db.insertCity(1, "Dhaka", 23.8103, 90.4125);
            db.insertCity(2, "Khulna", 22.8456, 89.5403);
            db.insertCity(3, "Rajshahi", 24.3636, 88.6241);
            db.insertCity(4, "Chittagong", 22.3569, 91.7832);
        }
    }
}