package com.easyride.location_service.controller;

import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.model.GeofenceType;
import com.easyride.location_service.service.GeofenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeofenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeofenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeofenceService geofenceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createGeofence_Success() throws Exception {
        Geofence geofence = new Geofence();
        geofence.setName("Test Zone");
        geofence.setType(GeofenceType.PICKUP_HOTSPOT);
        geofence.setPolygonCoordinatesJson("[[0,0], [0,1], [1,1], [1,0]]");

        when(geofenceService.createGeofence(any(Geofence.class))).thenReturn(geofence);

        mockMvc.perform(post("/api/geofences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(geofence)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)) // Assuming code 0 for success
                .andExpect(jsonPath("$.data.name").value("Test Zone"));
    }

    @Test
    void getAllActiveGeofences_Success() throws Exception {
        Geofence geofence = new Geofence();
        geofence.setName("Active Zone");
        when(geofenceService.getAllActiveGeofences()).thenReturn(List.of(geofence));

        mockMvc.perform(get("/api/geofences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Active Zone"));
    }

    @Test
    void getAllActiveGeofences_ByType_Success() throws Exception {
        Geofence geofence = new Geofence();
        geofence.setName("Airport Zone");
        when(geofenceService.getActiveGeofencesByType("PICKUP_HOTSPOT")).thenReturn(List.of(geofence));

        mockMvc.perform(get("/api/geofences").param("type", "PICKUP_HOTSPOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Airport Zone"));
    }

    @Test
    void checkPointInGeofences_Success() throws Exception {
        Geofence geofence = new Geofence();
        geofence.setName("Containing Zone");
        when(geofenceService.findGeofencesContainingPoint(anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(geofence));

        mockMvc.perform(get("/api/geofences/check")
                .param("lat", "40.0")
                .param("lon", "-74.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Containing Zone"));
    }
}
