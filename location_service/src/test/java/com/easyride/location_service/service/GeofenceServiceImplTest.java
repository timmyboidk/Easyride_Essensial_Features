package com.easyride.location_service.service;

import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.repository.GeofenceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceServiceImplTest {

    @Mock
    private GeofenceMapper geofenceMapper;

    private GeofenceServiceImpl geofenceService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceServiceImpl(geofenceMapper, objectMapper);
    }

    @Test
    void createGeofence_Success() {
        Geofence geofence = new Geofence();
        when(geofenceMapper.insert(any(Geofence.class))).thenReturn(1);

        Geofence result = geofenceService.createGeofence(geofence);
        assertNotNull(result);
    }

    @Test
    void isPointInGeofence_In() {
        Geofence geofence = new Geofence();
        geofence.setActive(true);
        // Square 0,0 to 10,10. Point 5,5 is inside.
        geofence.setPolygonCoordinatesJson("[[0.0, 0.0], [10.0, 0.0], [10.0, 10.0], [0.0, 10.0]]");

        when(geofenceMapper.selectById(1L)).thenReturn(geofence);

        boolean result = geofenceService.isPointInGeofence(5.0, 5.0, 1L);
        assertTrue(result);
    }

    @Test
    void isPointInGeofence_Out() {
        Geofence geofence = new Geofence();
        geofence.setActive(true);
        geofence.setPolygonCoordinatesJson("[[0.0, 0.0], [10.0, 0.0], [10.0, 10.0], [0.0, 10.0]]");

        when(geofenceMapper.selectById(1L)).thenReturn(geofence);

        boolean result = geofenceService.isPointInGeofence(15.0, 15.0, 1L);
        assertFalse(result);
    }
}
