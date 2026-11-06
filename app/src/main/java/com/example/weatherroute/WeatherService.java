package com.example.weatherroute;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherService {

    // NOTE: For a real app, register at openweathermap.org to get a free API key.
    // We are using a placeholder here. If the key is invalid, our catch block provides a fallback.
    private static final String API_KEY = "2ef6f60d803ae6cf711a31056386400f";

    public static String getLiveWeather(double lat, double lon) {
        String result = "Clear"; // Default fallback
        try {
            String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // JSON Parsing (Week 7 & 11)
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                JSONObject weatherObject = weatherArray.getJSONObject(0);
                result = weatherObject.getString("main"); // e.g., "Rain", "Clouds", "Clear"
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "Clear (Offline/Fallback)"; // Prevents crash if API key is missing
        }
        return result;
    }
}