package com.example.weatherroute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class RouteOptimizer {

    // Helper class for PriorityQueue
    private static class Node implements Comparable<Node> {
        int cityId;
        int distance;

        Node(int cityId, int distance) {
            this.cityId = cityId;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    public static List<RouteStep> calculateSafestRoute(City origin, City destination, List<City> allCities, List<Road> allRoads) {
        Map<Integer, Integer> distances = new HashMap<>();
        Map<Integer, Integer> previousNodes = new HashMap<>();
        Map<Integer, City> cityMap = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>();

        // Initialize maps
        for (City city : allCities) {
            distances.put(city.getId(), Integer.MAX_VALUE);
            cityMap.put(city.getId(), city);
        }

        distances.put(origin.getId(), 0);
        queue.add(new Node(origin.getId(), 0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentId = current.cityId;

            if (currentId == destination.getId()) break; // Reached destination
            if (current.distance > distances.get(currentId)) continue; // Obsolete node

            // Check all neighbors
            for (Road road : allRoads) {
                if (road.getFromCityId() == currentId) {
                    int neighborId = road.getToCityId();
                    City neighborCity = cityMap.get(neighborId);

                    // --- WEATHER ADAPTIVE LOGIC ---
                    // Fetch live weather for the neighbor city
                    String weather = WeatherService.getLiveWeather(neighborCity.getLat(), neighborCity.getLon());

                    // Apply penalty multiplier if bad weather (Rain/Snow = 1.5x longer travel time)
                    int weatherPenalty = road.getDistance();
                    if (weather.toLowerCase().contains("rain") || weather.toLowerCase().contains("snow")) {
                        weatherPenalty = (int) (road.getDistance() * 1.5);
                    }

                    int newDist = distances.get(currentId) + weatherPenalty;

                    if (newDist < distances.get(neighborId)) {
                        distances.put(neighborId, newDist);
                        previousNodes.put(neighborId, currentId);
                        queue.add(new Node(neighborId, newDist));
                    }
                }
            }
        }

        return buildRoutePath(origin, destination, previousNodes, cityMap, allRoads);
    }

    private static List<RouteStep> buildRoutePath(City origin, City destination, Map<Integer, Integer> previousNodes, Map<Integer, City> cityMap, List<Road> allRoads) {
        List<RouteStep> steps = new ArrayList<>();
        Integer curr = destination.getId();

        if (!previousNodes.containsKey(curr) && origin.getId() != destination.getId()) {
            steps.add(new RouteStep("No safe route found", "N/A", "N/A"));
            return steps;
        }

        List<Integer> pathIds = new ArrayList<>();
        while (curr != null) {
            pathIds.add(curr);
            curr = previousNodes.get(curr);
        }
        Collections.reverse(pathIds);

        // Convert Node IDs into UI RouteSteps
        for (int i = 0; i < pathIds.size() - 1; i++) {
            City from = cityMap.get(pathIds.get(i));
            City to = cityMap.get(pathIds.get(i + 1));

            // Find base distance for UI display
            int dist = 0;
            for (Road r : allRoads) {
                if (r.getFromCityId() == from.getId() && r.getToCityId() == to.getId()) {
                    dist = r.getDistance();
                    break;
                }
            }

            // Simulate one last weather check for the UI label
            String finalWeather = WeatherService.getLiveWeather(to.getLat(), to.getLon());

            steps.add(new RouteStep(
                    from.getName() + " → " + to.getName(),
                    "Distance: " + dist + " km",
                    "Weather: " + finalWeather
            ));
        }

        return steps;
    }
}