package com.easyride.location_service.service;

import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.model.GeofenceType;
import com.easyride.location_service.repository.GeofenceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofenceServiceImplTest {

    @Mock
    private GeofenceRepository geofenceRepository;

    private GeofenceServiceImpl geofenceService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        geofenceService = new GeofenceServiceImpl(geofenceRepository, objectMapper);
    }

    @Test
    void createGeofence_Success() {
        Geofence geofence = new Geofence();
        when(geofenceRepository.save(any(Geofence.class))).thenReturn(geofence);

        Geofence result = geofenceService.createGeofence(geofence);
        assertNotNull(result);
    }

    @Test
    void isPointInGeofence_In() {
        Geofence geofence = new Geofence();
        geofence.setActive(true);
        // Square 0,0 to 10,10. Point 5,5 is inside.
        geofence.setPolygonCoordinatesJson("[[0.0, 0.0], [10.0, 0.0], [10.0, 10.0], [0.0, 10.0]]");

        when(geofenceRepository.findById(1L)).thenReturn(Optional.of(geofence));

        boolean result = geofenceService.isPointInGeofence(5.0, 5.0, 1L);
        assertTrue(result);
    }

    @Test
    void isPointInGeofence_Out() {
        Geofence geofence = new Geofence();
        geofence.setActive(true);
        geofence.setPolygonCoordinatesJson("[[0.0, 0.0], [10.0, 0.0], [10.0, 10.0], [0.0, 10.0]]");

        when(geofenceRepository.findById(1L)).thenReturn(Optional.of(geofence));

        boolean result = geofenceService.isPointInGeofence(15.0, 15.0, 1L);
        assertFalse(result);
    }
}
