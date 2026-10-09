package com.example.weatherroute;

public class City {
    private int id;
    private String name;
    private double lat;
    private double lon;

    public City(int id, String name, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    @Override
    public String toString() {
        return name; // This helps the Spinner display the city name correctly
    }
}