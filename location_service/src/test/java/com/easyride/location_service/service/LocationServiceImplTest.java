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
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ListOperations<String, Object> listOps;

    @Mock
    private GeoOperations<String, Object> geoOps;

    @Mock
    private SafetyService safetyService;

    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @InjectMocks
    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        // Initialize the service with the real ObjectMapper but mocked others
        locationService = new LocationServiceImpl(restTemplate, redisTemplate, objectMapper);
        ReflectionTestUtils.setField(locationService, "googleMapsApiKey", "TEST_API_KEY");
        ReflectionTestUtils.setField(locationService, "safetyService", safetyService); // Inject SafetyService manually
                                                                                       // if needed or let @InjectMocks
                                                                                       // handle it if constructor
                                                                                       // injection was used for it?
        // Note: LocationServiceImpl code uses field injection for SafetyService, so
        // @InjectMocks might stick, but I'll use ReflectionTestUtils to be safe given
        // my previous edits/views might have mixed constructor/field injection.
        // Actually I changed LocationServiceImpl to only have constructor for 3 args,
        // SafetyService is Autowired field.

        // Mock the ops calls in @PostConstruct init()
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);

        // Trigger init manually because @PostConstruct doesn't run in unit tests
        // automatically unless using SpringExtension
        ReflectionTestUtils.invokeMethod(locationService, "init");
    }

    @Test
    void getLocationInfo_Success() {
        double lat = 37.7749;
        double lon = -122.4194;
        LocationResponse mockResponse = new LocationResponse(); // Assume empty is fine for checking not null

        when(restTemplate.getForEntity(anyString(), eq(LocationResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        LocationResponse result = locationService.getLocationInfo(lat, lon);

        assertNotNull(result);
        verify(restTemplate).getForEntity(contains("TEST_API_KEY"), eq(LocationResponse.class));
    }

    @Test
    void getLocationInfo_Failure() {
        when(restTemplate.getForEntity(anyString(), eq(LocationResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        LocationResponse result = locationService.getLocationInfo(0, 0);

        assertNull(result);
    }

    @Test
    void updateDriverLocation_Success() {
        Long driverId = 1L;
        DriverLocationUpdateDto dto = new DriverLocationUpdateDto();
        dto.setLatitude(10.0);
        dto.setLongitude(20.0);
        dto.setOrderId(100L);
        dto.setTimestamp(Instant.now());

        locationService.updateDriverLocation(driverId, dto);

        // Verify Geo Add
        verify(geoOps).add(eq("driver:locations"), any(Point.class), eq("1"));

        // Verify Value Set (Detail)
        verify(valueOps).set(eq("driver_location:1"), any(LocationDataDto.class), eq(300L), eq(TimeUnit.SECONDS));

        // Verify Trip Recording
        verify(listOps).rightPush(eq("trip_path:100"), anyString());

        // Verify Safety Check
        verify(safetyService).checkRouteDeviation(eq(100L), eq(1L), eq(10.0), eq(20.0));
    }

    @Test
    void getDriverLocation_Success() {
        Long driverId = 1L;
        LocationDataDto dto = new LocationDataDto(driverId, 10.0, 20.0, Instant.now(), null, null);

        when(valueOps.get("driver_location:1")).thenReturn(dto);

        LocationDataDto result = locationService.getDriverLocation(driverId);

        assertNotNull(result);
        assertEquals(driverId, result.getEntityId());
    }

    @Test
    void getDriverLocation_NotFound() {
        when(valueOps.get("driver_location:99")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> locationService.getDriverLocation(99L));
    }

    @Test
    void recordTripLocation_Success() throws Exception {
        Long orderId = 100L;
        Long driverId = 1L;
        locationService.recordTripLocation(orderId, driverId, 10.0, 20.0, Instant.now());

        verify(listOps).rightPush(eq("trip_path:100"), anyString());
        verify(redisTemplate).expire(eq("trip_path:100"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void getTripPath_Success() throws Exception {
        Long orderId = 100L;
        LocationDataDto dto = new LocationDataDto(1L, 10.0, 20.0, Instant.now(), orderId, null);
        String json = objectMapper.writeValueAsString(dto);

        when(listOps.range("trip_path:100", 0, -1)).thenReturn(Collections.singletonList(json));

        List<LocationDataDto> result = locationService.getTripPath(orderId);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(10.0, result.get(0).getLatitude());
    }
}
