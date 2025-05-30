package com.easyride.location_service.controller;

import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/info")
    public LocationResponse getLocationInfo(@RequestParam double latitude, @RequestParam double longitude) {
        return locationService.getLocationInfo(latitude, longitude);
    }
}
