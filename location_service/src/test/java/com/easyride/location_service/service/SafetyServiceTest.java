package com.easyride.location_service.service;

import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.PlannedRoute;
import com.easyride.location_service.rocket.LocationAlertProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafetyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private LocationAlertProducer alertProducer;

    private SafetyService safetyService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        safetyService = new SafetyService(redisTemplate, objectMapper, alertProducer);
        ReflectionTestUtils.setField(safetyService, "deviationThresholdMeters", 500.0);
        ReflectionTestUtils.setField(safetyService, "alertIntervalSeconds", 60L);
    }

    @Test
    void storePlannedRoute_Success() throws Exception {
        PlannedRoute route = new PlannedRoute();
        route.setOrderId(1L);
        route.setWaypoints(List.of(new LocationDataDto()));

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        safetyService.storePlannedRoute(route);

        verify(valueOperations, times(1)).set(anyString(), anyString());
    }

    @Test
    void checkRouteDeviation_NoRoute() {
        when(valueOperations.get(anyString())).thenReturn(null);
        safetyService.checkRouteDeviation(1L, 100L, 40.0, -74.0);
        verify(alertProducer, never()).sendRouteDeviationAlert(any());
    }

    @Test
    void checkRouteDeviation_DriverMismatch() throws Exception {
        PlannedRoute route = new PlannedRoute();
        route.setOrderId(1L);
        route.setDriverId(200L); // Expected driver
        route.setWaypoints(List.of(new LocationDataDto()));

        when(valueOperations.get(anyString())).thenReturn("json_string");
        when(objectMapper.readValue(anyString(), eq(PlannedRoute.class))).thenReturn(route);

        safetyService.checkRouteDeviation(1L, 300L, 40.0, -74.0); // Wrong driver
        verify(alertProducer, never()).sendRouteDeviationAlert(any());
    }

    // Test detection logic would require mocking HaversineUtil or using actual
    // logic (since HaversineUtil is static and simple).
    // Simulating deviation: set deviationThresholdMeters to small value, route
    // point far away.
    @Test
    void checkRouteDeviation_Detected() throws Exception {
        PlannedRoute route = new PlannedRoute();
        route.setOrderId(1L);
        route.setDriverId(200L);
        LocationDataDto wp = new LocationDataDto();
        wp.setLatitude(40.0);
        wp.setLongitude(-74.0);
        route.setWaypoints(List.of(wp));

        when(valueOperations.get(anyString())).thenReturn("json_string");
        when(objectMapper.readValue(anyString(), eq(PlannedRoute.class))).thenReturn(route);

        // Current location far away (degree diff ~ 111km)
        safetyService.checkRouteDeviation(1L, 200L, 41.0, -74.0);

        verify(alertProducer, times(1)).sendRouteDeviationAlert(any());
    }
}
