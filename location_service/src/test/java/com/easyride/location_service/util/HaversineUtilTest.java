package com.easyride.location_service.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HaversineUtilTest {

    @Test
    void distance_Success() {
        // New York to Philadelphia (~130km = 130000m)
        double lat1 = 40.7128;
        double lon1 = -74.0060;
        double lat2 = 39.9526;
        double lon2 = -75.1652;

        double distance = HaversineUtil.distance(lat1, lon1, lat2, lon2);

        // Assert within range (meters)
        assertEquals(130000.0, distance, 10000.0);
    }
}
