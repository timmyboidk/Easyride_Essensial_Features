package com.easyride.location_service.controller;

import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getLocationInfo_Success() throws Exception {
        LocationResponse response = new LocationResponse();
        response.setStatus("OK");

        when(locationService.getLocationInfo(anyDouble(), anyDouble())).thenReturn(response);

        mockMvc.perform(get("/api/location/info")
                .param("lat", "37.7749")
                .param("lon", "-122.4194"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OK"));
    }

    @Test
    void updateDriverLocation_Success() throws Exception {
        DriverLocationUpdateDto dto = new DriverLocationUpdateDto();
        dto.setLatitude(37.7749);
        dto.setLongitude(-122.4194);

        doNothing().when(locationService).updateDriverLocation(anyLong(), any(DriverLocationUpdateDto.class));

        mockMvc.perform(post("/api/location/driver/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("司机位置更新成功"));
    }

    @Test
    void getDriverLocation_Success() throws Exception {
        LocationDataDto dto = new LocationDataDto();
        dto.setEntityId(1L);
        dto.setLatitude(37.7749);

        when(locationService.getDriverLocation(anyLong())).thenReturn(dto);

        mockMvc.perform(get("/api/location/driver/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(37.7749));
    }

    @Test
    void getOrderTripPath_Success() throws Exception {
        LocationDataDto dto = new LocationDataDto();
        dto.setEntityId(1L);

        when(locationService.getTripPath(anyLong())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/location/trip/100/path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].entityId").value(1));
    }
}
