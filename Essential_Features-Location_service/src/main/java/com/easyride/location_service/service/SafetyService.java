package com.easyride.location_service.service;

import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.dto.RouteDeviationAlertDto;
import com.easyride.location_service.model.PlannedRoute;
import com.easyride.location_service.rocket.LocationAlertProducer;
import com.easyride.location_service.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate; // For storing planned routes
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class SafetyService {
    private static final Logger log = LoggerFactory.getLogger(SafetyService.class);

    // Store planned routes in Redis, keyed by orderId
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LocationAlertProducer alertProducer;

    @Value("${easyride.safety.route-deviation-threshold-meters:500}") // Default 500m
    private double deviationThresholdMeters;

    @Value("${easyride.safety.route-deviation-alert-interval-seconds:60}")
    private long alertIntervalSeconds;

    // Cache last alert time per order to avoid spamming
    private final Map<Long, Instant> lastAlertTimes = new ConcurrentHashMap<>();
    private static final String PLANNED_ROUTE_KEY_PREFIX = "planned_route:";


    @Autowired
    public SafetyService(RedisTemplate<String, Object> redisTemplate,
                         ObjectMapper objectMapper,
                         LocationAlertProducer alertProducer) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.alertProducer = alertProducer;
    }

    // Method to be called by OrderEventsConsumer when a trip starts with a planned route
    public void storePlannedRoute(PlannedRoute plannedRoute) {
        if (plannedRoute == null || plannedRoute.getWaypoints() == null || plannedRoute.getWaypoints().isEmpty()) {
            log.warn("Cannot store null or empty planned route for order {}", plannedRoute != null ? plannedRoute.getOrderId() : "null");
            return;
        }
        try {
            String key = PLANNED_ROUTE_KEY_PREFIX + plannedRoute.getOrderId();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(plannedRoute));
            log.info("Stored planned route for order {}", plannedRoute.getOrderId());
        } catch (Exception e) {
            log.error("Error storing planned route for order {}: ", plannedRoute.getOrderId(), e);
        }
    }

    public void removePlannedRoute(Long orderId) {
        String key = PLANNED_ROUTE_KEY_PREFIX + orderId;
        redisTemplate.delete(key);
        lastAlertTimes.remove(orderId); // Clear alert cache too
        log.info("Removed planned route for order {} (e.g. trip ended/cancelled)", orderId);
    }


    // This method would be triggered by new driver location updates (e.g., from LocationServiceImpl or an event)
    public void checkRouteDeviation(Long orderId, Long driverId, double currentLat, double currentLon) {
        PlannedRoute plannedRoute = getPlannedRoute(orderId);
        if (plannedRoute == null || plannedRoute.getWaypoints() == null || plannedRoute.getWaypoints().isEmpty()) {
            // log.debug("No planned route found for order {} to check deviation.", orderId);
            return;
        }
        if (!driverId.equals(plannedRoute.getDriverId())) {
            log.warn("Driver ID mismatch for route deviation check. Order: {}, Expected Driver: {}, Current Driver: {}",
                    orderId, plannedRoute.getDriverId(), driverId);
            return;
        }

        double minDistanceToRoute = Double.MAX_VALUE;
        for (LocationDataDto waypoint : plannedRoute.getWaypoints()) {
            double dist = HaversineUtil.distance(currentLat, currentLon, waypoint.getLatitude(), waypoint.getLongitude());
            if (dist < minDistanceToRoute) {
                minDistanceToRoute = dist;
            }
        }
        // Note: This is distance to nearest waypoint, not necessarily perpendicular distance to route segment.
        // For better accuracy, iterate through segments (pairs of waypoints) and calculate perpendicular distance.
        // For now, this simpler check:

        log.debug("Order {}: Driver {} at ({}, {}). Min distance to planned route waypoints: {} meters.",
                orderId, driverId, currentLat, currentLon, minDistanceToRoute);

        if (minDistanceToRoute > deviationThresholdMeters) {
            log.warn("ROUTE DEVIATION DETECTED for order {}: Driver {} is {}m off route (threshold {}m).",
                    orderId, driverId, minDistanceToRoute, deviationThresholdMeters);

            Instant lastAlert = lastAlertTimes.get(orderId);
            if (lastAlert == null || Duration.between(lastAlert, Instant.now()).getSeconds() > alertIntervalSeconds) {
                RouteDeviationAlertDto alert = new RouteDeviationAlertDto(
                        orderId, driverId, currentLat, currentLon,
                        minDistanceToRoute, Instant.now(),
                        "司机可能已偏离规划路线。"
                );
                alertProducer.sendRouteDeviationAlert(alert);
                lastAlertTimes.put(orderId, Instant.now());
                log.info("Route deviation alert sent for order {}", orderId);
            } else {
                log.info("Route deviation alert for order {} suppressed due to alert interval.", orderId);
            }
        }
    }

    private PlannedRoute getPlannedRoute(Long orderId) {
        try {
            String key = PLANNED_ROUTE_KEY_PREFIX + orderId;
            String routeJson = (String) redisTemplate.opsForValue().get(key);
            if (routeJson != null) {
                return objectMapper.readValue(routeJson, PlannedRoute.class);
            }
        } catch (Exception e) {
            log.error("Error fetching planned route for order {}: ", orderId, e);
        }
        return null;
    }
}