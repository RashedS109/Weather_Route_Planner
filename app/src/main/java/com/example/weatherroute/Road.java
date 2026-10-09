package com.example.weatherroute;

public class Road {
    private int fromCityId;
    private int toCityId;
    private int distance; // Base weight (kilometers)

    public Road(int fromCityId, int toCityId, int distance) {
        this.fromCityId = fromCityId;
        this.toCityId = toCityId;
        this.distance = distance;
    }

    public int getFromCityId() { return fromCityId; }
    public int getToCityId() { return toCityId; }
    public int getDistance() { return distance; }
}