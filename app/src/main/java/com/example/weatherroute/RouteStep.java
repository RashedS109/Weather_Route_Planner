package com.example.weatherroute;

public class RouteStep {
    private String segment;
    private String distance;
    private String weatherCondition;

    public RouteStep(String segment, String distance, String weatherCondition) {
        this.segment = segment;
        this.distance = distance;
        this.weatherCondition = weatherCondition;
    }

    public String getSegment() { return segment; }
    public String getDistance() { return distance; }
    public String getWeatherCondition() { return weatherCondition; }
}