package com.easyride.location_service.service;

import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.geo.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Map;

@Service
public class LocationServiceImpl implements LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);
    private static final String DRIVER_LOCATION_KEY_PREFIX = "driver_location:";
    private static final String DRIVER_GEO_KEY = "driver:locations";
    private static final String TRIP_PATH_KEY_PREFIX = "trip_path:";
    private static final long DRIVER_LOCATION_TTL_SECONDS = 300; // Location valid for 5 mins

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOps;
    private ListOperations<String, Object> listOps;
    private GeoOperations<String, Object> geoOps;
    private final ObjectMapper objectMapper;

    private final SafetyService safetyService;

    @Autowired
    public LocationServiceImpl(RestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper, SafetyService safetyService) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.safetyService = safetyService;
    }

    @PostConstruct
    void init() {
        this.valueOps = redisTemplate.opsForValue();
        this.listOps = redisTemplate.opsForList();
        this.geoOps = redisTemplate.opsForGeo();
    }

    @Override
    public LocationResponse getLocationInfo(double latitude, double longitude) {
        String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
                latitude, longitude, googleMapsApiKey);
        log.info("Requesting geocoding from Google Maps API for lat={}, lon={}", latitude, longitude);
        ResponseEntity<LocationResponse> response = restTemplate.getForEntity(url, LocationResponse.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("Geocoding successful: {}", response.getBody());
            return response.getBody();
        } else {
            log.error("Error fetching location info from Google Maps. Status: {}, Body: {}", response.getStatusCode(),
                    response.getBody());
            return null;
        }
    }

    @Override
    public void updateDriverLocation(Long driverId, DriverLocationUpdateDto updateDto) {
        Instant timestamp = updateDto.getTimestamp() != null ? updateDto.getTimestamp() : Instant.now();

        // 1. Update Geospatial Index (driver:locations)
        // MEMBER is driverId (as String)
        geoOps.add(DRIVER_GEO_KEY, new Point(updateDto.getLongitude(), updateDto.getLatitude()),
                String.valueOf(driverId));

        // 2. Store detailed info (driver_location:{id}) for full metadata access
        // This is needed because GEO only stores coords + member.
        String key = DRIVER_LOCATION_KEY_PREFIX + driverId;
        LocationDataDto locationData = new LocationDataDto(
                driverId,
                updateDto.getLatitude(),
                updateDto.getLongitude(),
                timestamp,
                updateDto.getOrderId(),
                null);

        valueOps.set(key, locationData, DRIVER_LOCATION_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Updated location for driver {}: {}", driverId, locationData);

        // 3. Record path if on a trip
        if (updateDto.getOrderId() != null) {
            recordTripLocation(updateDto.getOrderId(), driverId, updateDto.getLatitude(), updateDto.getLongitude(),
                    timestamp);
            safetyService.checkRouteDeviation(updateDto.getOrderId(), driverId, updateDto.getLatitude(),
                    updateDto.getLongitude());
        }
    }

    @Override
    public LocationDataDto getDriverLocation(Long driverId) {
        String key = DRIVER_LOCATION_KEY_PREFIX + driverId;
        Object rawData = valueOps.get(key);
        if (rawData == null) {
            // Fallback: Try to get from Geo index if the detail key expired but Geo didn't
            // (unlikely but possible)
            // But w/o metadata, it's partial. Let's just return null or throw.
            log.warn("No location data found for driver {}", driverId);
            throw new ResourceNotFoundException("Driver " + driverId + " location not found or expired");
        }

        if (rawData instanceof LocationDataDto) {
            return (LocationDataDto) rawData;
        } else if (rawData instanceof Map) {
            try {
                return objectMapper.convertValue(rawData, LocationDataDto.class);
            } catch (IllegalArgumentException e) {
                log.error("Error converting stored Redis data for driver {} to LocationDataDto: {}", driverId, rawData,
                        e);
                throw new IllegalStateException("Invalid location data format");
            }
        }
        log.error("Unexpected data type in Redis for driver {}: {}", driverId, rawData.getClass().getName());
        throw new IllegalStateException("Invalid location data format");
    }

    @Override
    public void recordTripLocation(Long orderId, Long driverId, double latitude, double longitude, Instant timestamp) {
        String tripKey = TRIP_PATH_KEY_PREFIX + orderId;
        LocationDataDto tripPoint = new LocationDataDto(driverId, latitude, longitude, timestamp, orderId, null);
        try {
            listOps.rightPush(tripKey, objectMapper.writeValueAsString(tripPoint));
            redisTemplate.expire(tripKey, 24, TimeUnit.HOURS); // Good practice to have TTL
            log.debug("Recorded trip location for order {}: {}", orderId, tripPoint);
        } catch (Exception e) {
            log.error("Error serializing trip point for order {}: ", orderId, e);
        }
    }

    @Override
    public List<LocationDataDto> getTripPath(Long orderId) {
        String tripKey = TRIP_PATH_KEY_PREFIX + orderId;
        List<Object> rawPath = listOps.range(tripKey, 0, -1);
        if (rawPath == null || rawPath.isEmpty()) {
            return Collections.emptyList();
        }
        return rawPath.stream()
                .map(obj -> {
                    try {
                        if (obj instanceof String) {
                            return objectMapper.readValue((String) obj, LocationDataDto.class);
                        } else if (obj instanceof Map) {
                            return objectMapper.convertValue(obj, LocationDataDto.class);
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Error deserializing trip point for order {}: ", orderId, e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}