package com.example.weatherroute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class RouteOptimizer {

    public enum RoutingMode {
        BALANCED,      // Combines weather and distance into one score
        SAFEST_FIRST,  // Primary: Weather, Secondary: Distance
        SHORTEST_FIRST // Primary: Distance, Secondary: Weather
    }

    public interface AlgorithmStepListener {
        void onEdgeEvaluated(City fromCity, City toCity);
    }

    // Advanced Data Structure: Multi-Objective Cost Tuple
    static class LexicographicCost implements Comparable<LexicographicCost> {
        double primary;
        double secondary;

        public LexicographicCost(double primary, double secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        public LexicographicCost add(double edgePrimary, double edgeSecondary) {
            return new LexicographicCost(this.primary + edgePrimary, this.secondary + edgeSecondary);
        }

        @Override
        public int compareTo(LexicographicCost other) {
            // Strict Hierarchy: Only look at secondary if primary costs are identical
            if (Math.abs(this.primary - other.primary) > 0.001) {
                return Double.compare(this.primary, other.primary);
            }
            return Double.compare(this.secondary, other.secondary);
        }
    }

    static class Node implements Comparable<Node> {
        City city;
        LexicographicCost cost;

        public Node(City city, LexicographicCost cost) {
            this.city = city;
            this.cost = cost;
        }

        @Override
        public int compareTo(Node other) {
            return this.cost.compareTo(other.cost);
        }
    }

    public static List<RouteStep> calculateOptimalRoute(City origin, City destination, List<City> allCities, List<Road> allRoads, RoutingMode mode, AlgorithmStepListener listener) {        Map<Integer, List<Road>> adjList = new HashMap<>();
        for (City c : allCities) adjList.put(c.getId(), new ArrayList<>());
        for (Road r : allRoads) adjList.get(r.getFromCityId()).add(r);

        Map<Integer, LexicographicCost> distances = new HashMap<>();
        Map<Integer, City> predecessors = new HashMap<>();
        Map<Integer, Double> weatherPenalties = new HashMap<>();

        PriorityQueue<Node> pq = new PriorityQueue<>();

        // Initialize distances to infinity
        for (City c : allCities) {
            distances.put(c.getId(), new LexicographicCost(Double.MAX_VALUE, Double.MAX_VALUE));
        }

        distances.put(origin.getId(), new LexicographicCost(0, 0));
        pq.add(new Node(origin, new LexicographicCost(0, 0)));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            City u = current.city;

            if (u.getId() == destination.getId()) break; // Target reached

            for (Road edge : adjList.get(u.getId())) {
                City v = getCityById(allCities, edge.getToCityId());
                if (listener != null) {
                    listener.onEdgeEvaluated(u, v);
                    try {
                        Thread.sleep(400); // Pause for 400 milliseconds so the human eye can watch it "think"
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // Fetch simulated or live weather penalty
                double weatherPenalty = fetchWeatherPenalty(v);
                weatherPenalties.put(v.getId(), weatherPenalty);

                double edgePrimary = 0;
                double edgeSecondary = 0;

                // Apply Multi-Objective Weights based on User Selection
                switch (mode) {
                    case SAFEST_FIRST:
                        edgePrimary = weatherPenalty;       // Objective 1
                        edgeSecondary = edge.getDistance(); // Objective 2
                        break;
                    case SHORTEST_FIRST:
                        edgePrimary = edge.getDistance();   // Objective 1
                        edgeSecondary = weatherPenalty;     // Objective 2
                        break;
                    case BALANCED:
                        edgePrimary = edge.getDistance() + (weatherPenalty * 10); // Standard combined formula
                        edgeSecondary = 0;
                        break;
                }

                LexicographicCost newCost = distances.get(u.getId()).add(edgePrimary, edgeSecondary);

                // Relaxation phase using the custom Comparable interface
                if (newCost.compareTo(distances.get(v.getId())) < 0) {
                    distances.put(v.getId(), newCost);
                    predecessors.put(v.getId(), u);
                    pq.add(new Node(v, newCost));
                }
            }
        }

        // Reconstruct Path
        List<RouteStep> route = new ArrayList<>();
        City curr = destination;
        while (predecessors.containsKey(curr.getId())) {
            City prev = predecessors.get(curr.getId());
            double dist = getRoadDistance(allRoads, prev.getId(), curr.getId());
            route.add(new RouteStep(prev.getName() + " -> " + curr.getName(), "Penalty: " + weatherPenalties.get(curr.getId()), String.format("%.1f km", dist)));
            curr = prev;
        }

        Collections.reverse(route);
        if (route.isEmpty()) {
            route.add(new RouteStep("No safe route found", "N/A", "0 km"));        }
        return route;
    }

    private static double fetchWeatherPenalty(City c) {
        // Replace with your actual OpenWeather API call logic
        // Returning a random penalty for demonstration
        return Math.random() * 50;
    }

    private static City getCityById(List<City> cities, int id) {
        for (City c : cities) if (c.getId() == id) return c;
        return null;
    }

    private static double getRoadDistance(List<Road> roads, int fromId, int toId) {
        for (Road r : roads) if (r.getFromCityId() == fromId && r.getToCityId() == toId) return r.getDistance();
        return 0;
    }
    // ------------------------------------------------------------------------
    // ADVANCED ALGORITHM: Bellman-Ford for Dynamic/Severe Weight Changes
    // Time Complexity: O(V * E)
    // ------------------------------------------------------------------------
    public static List<RouteStep> calculateRouteBellmanFord(City origin, City destination, List<City> allCities, List<Road> allRoads, Map<Integer, Double> activeStorms) {
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();

        // 1. Initialize distances to infinity
        for (City c : allCities) {
            distances.put(c.getId(), Double.MAX_VALUE);
        }
        distances.put(origin.getId(), 0.0);

        int V = allCities.size();

        // 2. Relax all edges |V| - 1 times
        for (int i = 1; i < V; i++) {
            for (Road edge : allRoads) {
                int u = edge.getFromCityId();
                int v = edge.getToCityId();

                if (distances.get(u) == Double.MAX_VALUE) continue;

                // Combine baseline penalty with dynamic sudden storm penalty
                double basePenalty = fetchWeatherPenalty(getCityById(allCities, v));
                double stormPenalty = activeStorms.containsKey(v) ? activeStorms.get(v) : 0.0;
                double totalWeight = edge.getDistance() + basePenalty + stormPenalty;

                // Bellman-Ford Relaxation
                if (distances.get(u) + totalWeight < distances.get(v)) {
                    distances.put(v, distances.get(u) + totalWeight);
                    predecessors.put(v, u);
                }
            }
        }

        // 3. Path Reconstruction
        List<RouteStep> route = new ArrayList<>();
        int curr = destination.getId();

        while (predecessors.containsKey(curr)) {
            int prev = predecessors.get(curr);
            double dist = getRoadDistance(allRoads, prev, curr);

            String weatherText = activeStorms.containsKey(curr) ? "⚠️ SEVERE STORM (+9999 cost)" : "Clear / Base Penalty";

            route.add(new RouteStep(
                    getCityById(allCities, prev).getName() + " -> " + getCityById(allCities, curr).getName(),
                    weatherText,
                    String.format("%.1f km", dist)
            ));
            curr = prev;
        }

        Collections.reverse(route);
        if (route.isEmpty() || distances.get(destination.getId()) == Double.MAX_VALUE) {
            route.add(new RouteStep("No safe route found", "N/A", "0 km"));
        }
        return route;
    }
}