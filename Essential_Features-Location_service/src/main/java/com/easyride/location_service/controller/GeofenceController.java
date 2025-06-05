package com.easyride.location_service.controller;

import com.easyride.location_service.dto.ApiResponse;
import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.service.GeofenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geofences") // Typically managed by Admin service or an internal tool
public class GeofenceController {
    private static final Logger log = LoggerFactory.getLogger(GeofenceController.class);
    private final GeofenceService geofenceService;

    @Autowired
    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @PostMapping
    public ApiResponse<Geofence> createGeofence(@Valid @RequestBody Geofence geofence) {
        log.info("Request to create geofence: {}", geofence.getName());
        Geofence createdGeofence = geofenceService.createGeofence(geofence);
        return ApiResponse.success("地理围栏创建成功", createdGeofence);
    }

    @GetMapping
    public ApiResponse<List<Geofence>> getAllActiveGeofences(@RequestParam(required = false) String type) {
        log.info("Request to get all active geofences, type filter: {}", type);
        List<Geofence> geofences = (type != null && !type.isBlank()) ?
                geofenceService.getActiveGeofencesByType(type) :
                geofenceService.getAllActiveGeofences();
        return ApiResponse.success(geofences);
    }

    @GetMapping("/check")
    public ApiResponse<List<Geofence>> checkPointInGeofences(@RequestParam double lat, @RequestParam double lon, @RequestParam(required = false) String type) {
        log.info("Checking point ({},{}) in geofences of type filter: {}", lat, lon, type);
        List<Geofence> containingGeofences = geofenceService.findGeofencesContainingPoint(lat, lon, type);
        return ApiResponse.success(containingGeofences);
    }

    // Add PUT for update, DELETE for removal (ensure proper authorization)
}