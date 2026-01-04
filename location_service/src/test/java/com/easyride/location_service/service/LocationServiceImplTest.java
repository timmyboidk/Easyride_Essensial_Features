package com.easyride.location_service.service;

import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SafetyService safetyService;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ListOperations<String, Object> listOps;

    @Mock
    private GeoOperations<String, Object> geoOps;

    @InjectMocks
    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        locationService.init();
    }

    @Test
    void getLocationInfo_Success() {
        LocationResponse mockResponse = new LocationResponse();
        LocationResponse.Result resultPart = new LocationResponse.Result();
        resultPart.setFormatted_address("123 Main St");
        mockResponse.setResults(Collections.singletonList(resultPart));
        mockResponse.setStatus("OK");

        when(restTemplate.getForEntity(anyString(), eq(LocationResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        LocationResponse result = locationService.getLocationInfo(37.7749, -122.4194);

        assertNotNull(result);
        assertEquals("123 Main St", result.getResults().get(0).getFormatted_address());
    }

    @Test
    void updateDriverLocation_Success() {
        DriverLocationUpdateDto updateDto = new DriverLocationUpdateDto();
        updateDto.setLatitude(37.7749);
        updateDto.setLongitude(-122.4194);
        updateDto.setOrderId(100L);
        updateDto.setTimestamp(Instant.now());

        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (Exception e) {
            fail("Mock setup failed");
        }

        locationService.updateDriverLocation(1L, updateDto);

        verify(geoOps).add(eq("driver:locations"), any(Point.class), eq("1"));
        verify(valueOps).set(eq("driver_location:1"), any(LocationDataDto.class), anyLong(), any(TimeUnit.class));
        verify(listOps).rightPush(eq("trip_path:100"), anyString());
        verify(safetyService).checkRouteDeviation(100L, 1L, 37.7749, -122.4194);
    }

    @Test
    void getDriverLocation_Success() {
        LocationDataDto dto = new LocationDataDto();
        dto.setEntityId(1L);
        dto.setLatitude(37.7749);

        when(valueOps.get("driver_location:1")).thenReturn(dto);

        LocationDataDto result = locationService.getDriverLocation(1L);

        assertNotNull(result);
        assertEquals(37.7749, result.getLatitude());
    }

    @Test
    void getDriverLocation_NotFound() {
        when(valueOps.get("driver_location:1")).thenReturn(null);
        // Assuming geo location check is skipped if valueOps fails or not mocked to
        // return nearby
        // The service logic tries geo index ONLY if valueOps fails?
        // Code: if (rawData == null) { log.warn... throw ResourceNotFoundException }

        assertThrows(ResourceNotFoundException.class, () -> locationService.getDriverLocation(1L));
    }

    @Test
    void getTripPath_Success() throws Exception {
        String jsonPoint = "{\"entityId\":1}";
        LocationDataDto dto = new LocationDataDto();
        dto.setEntityId(1L);

        when(listOps.range("trip_path:100", 0, -1)).thenReturn(Collections.singletonList(jsonPoint));
        when(objectMapper.readValue(eq(jsonPoint), eq(LocationDataDto.class))).thenReturn(dto);

        List<LocationDataDto> path = locationService.getTripPath(100L);

        assertFalse(path.isEmpty());
        assertEquals(1L, path.get(0).getEntityId());
    }
}
