package com.easyride.matching_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HaversineDistanceUtilTest {

    @Test
    void calculateDistance_Success() {
        double lat1 = 40.7128;
        double lon1 = -74.0060;
        double lat2 = 39.9526;
        double lon2 = -75.1652;

        double distance = HaversineDistanceUtil.calculateDistance(lat1, lon1, lat2, lon2);

        // Expect ~130km
        assertEquals(130.0, distance, 10.0);
    }
}
