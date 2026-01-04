package com.easyride.order_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceCalculatorTest {

    @Test
    void calculateDistance_Success() {
        // Approximate coordinates for New York and Philadelphia
        double lat1 = 40.7128; // NY
        double lon1 = -74.0060;
        double lat2 = 39.9526; // Philly
        double lon2 = -75.1652;

        double distance = DistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        // Distance is roughly 130 km
        assertEquals(130.0, distance, 10.0);
    }

    @Test
    void calculateDistance_SameLocation() {
        double lat = 40.7128;
        double lon = -74.0060;

        double distance = DistanceCalculator.calculateDistance(lat, lon, lat, lon);

        assertEquals(0.0, distance, 0.001);
    }
}
