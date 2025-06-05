package com.easyride.location_service.service;

import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse; // Existing
import com.easyride.location_service.exception.ResourceNotFoundException; // Create this
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations; // For trip path
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Or WebClient

import jakarta.annotation.PostConstruct; // if using ValueOperations bean
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);
    private static final String DRIVER_LOCATION_KEY_PREFIX = "driver_location:";
    private static final String TRIP_PATH_KEY_PREFIX = "trip_path:";
    private static final long DRIVER_LOCATION_TTL_SECONDS = 300; // Location valid for 5 mins

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate; // Generic or specific type like <String, LocationDataDto>
    private ValueOperations<String, Object> valueOps;
    private ListOperations<String, Object> listOps; // For storing trip paths
    private final ObjectMapper objectMapper;


    @Autowired
    public LocationServiceImpl(RestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        valueOps = redisTemplate.opsForValue();
        listOps = redisTemplate.opsForList();
    }


    @Override
    public LocationResponse getLocationInfo(double latitude, double longitude) {
        // ... (existing geocoding logic using Google Maps HTTP API - keep as is)
        // This method seems fine from your existing code.
        String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                latitude, longitude, googleMapsApiKey);
        log.info("Requesting geocoding from Google Maps API for lat={}, lon={}", latitude, longitude);
        ResponseEntity<LocationResponse> response = restTemplate.getForEntity(url, LocationResponse.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("Geocoding successful: {}", response.getBody());
            return response.getBody();
        } else {
            log.error("Error fetching location info from Google Maps. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            // Consider throwing a specific exception
            return null;
        }
    }

    @Override
    public void updateDriverLocation(Long driverId, DriverLocationUpdateDto updateDto) {
        String key = DRIVER_LOCATION_KEY_PREFIX + driverId;
        Instant timestamp = updateDto.getTimestamp() != null ? updateDto.getTimestamp() : Instant.now();

        LocationDataDto locationData = new LocationDataDto(
                driverId,
                updateDto.getLatitude(),
                updateDto.getLongitude(),
                timestamp,
                updateDto.getOrderId(), // Store orderId if provided
                null // Formatted address not typically stored with every real-time update
        );

        valueOps.set(key, locationData, DRIVER_LOCATION_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Updated location for driver {}: {}", driverId, locationData);

        // If an order is active, also record it as part of the trip path
        if (updateDto.getOrderId() != null) {
            recordTripLocation(updateDto.getOrderId(), driverId, updateDto.getLatitude(), updateDto.getLongitude(), timestamp);
        }

        // TODO: Consider publishing a DRIVER_LOCATION_UPDATED event to a topic
        // if other services need to react in real-time (e.g., for map display on passenger app via WebSockets)
        // Example: rocketMQTemplate.convertAndSend("driver-location-stream-topic", locationData);
    }

    @Override
    public LocationDataDto getDriverLocation(Long driverId) {
        String key = DRIVER_LOCATION_KEY_PREFIX + driverId;
        Object rawData = valueOps.get(key);
        if (rawData == null) {
            log.warn("No location data found for driver {}", driverId);
            throw new ResourceNotFoundException("司机 " + driverId + " 的位置信息未找到或已过期");
        }
        // Due to type erasure with generic RedisTemplate<String, Object>, manual conversion is safer
        if (rawData instanceof LocationDataDto) {
            return (LocationDataDto) rawData;
        } else if (rawData instanceof Map) { // If Jackson stored it as a map
            try {
                return objectMapper.convertValue(rawData, LocationDataDto.class);
            } catch (IllegalArgumentException e) {
                log.error("Error converting stored Redis data for driver {} to LocationDataDto: {}", driverId, rawData, e);
                throw new IllegalStateException("存储的位置数据格式不正确");
            }
        }
        log.error("Unexpected data type in Redis for driver {}: {}", driverId, rawData.getClass().getName());
        throw new IllegalStateException("存储的位置数据格式不正确");
    }

    @Override
    public void recordTripLocation(Long orderId, Long driverId, double latitude, double longitude, Instant timestamp) {
        String tripKey = TRIP_PATH_KEY_PREFIX + orderId;
        LocationDataDto tripPoint = new LocationDataDto(driverId, latitude, longitude, timestamp, orderId, null);
        // Store as JSON string or use Redis specific structures if more complex queries are needed on path.
        // For simple path retrieval, list of JSON strings is okay.
        try {
            listOps.rightPush(tripKey, objectMapper.writeValueAsString(tripPoint));
            // Optionally set TTL for trip paths if they don't need to be stored indefinitely
            // redisTemplate.expire(tripKey, 24, TimeUnit.HOURS);
            log.debug("Recorded trip location for order {}: {}", orderId, tripPoint);
        } catch (Exception e) {
            log.error("Error serializing trip point for order {}: ", orderId, e);
        }
    }

    @Override
    public List<LocationDataDto> getTripPath(Long orderId) {
        String tripKey = TRIP_PATH_KEY_PREFIX + orderId;
        List<Object> rawPath = listOps.range(tripKey, 0, -1); // Get all elements
        if (rawPath == null || rawPath.isEmpty()) {
            return Collections.emptyList();
        }
        return rawPath.stream()
                .map(obj -> {
                    try {
                        if (obj instanceof String) {
                            return objectMapper.readValue((String) obj, LocationDataDto.class);
                        } else if (obj instanceof Map) { // Fallback if Redis stores it as map
                            return objectMapper.convertValue(obj, LocationDataDto.class);
                        }
                        log.warn("Unexpected object type in trip path for order {}: {}", orderId, obj.getClass());
                        return null;
                    } catch (Exception e) {
                        log.error("Error deserializing trip point for order {}: ", orderId, e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Autowired // Add SafetyService
    private SafetyService safetyService;

    @Override
    public void updateDriverLocation(Long driverId, DriverLocationUpdateDto updateDto) {
        // ... (existing update logic) ...
        valueOps.set(key, locationData, DRIVER_LOCATION_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Updated location for driver {}: {}", driverId, locationData);

        if (updateDto.getOrderId() != null) {
            recordTripLocation(updateDto.getOrderId(), driverId, updateDto.getLatitude(), updateDto.getLongitude(), timestamp);
            // Check for route deviation if an order is active
            safetyService.checkRouteDeviation(updateDto.getOrderId(), driverId, updateDto.getLatitude(), updateDto.getLongitude());
        }
}