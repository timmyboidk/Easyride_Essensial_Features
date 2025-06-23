package com.easyride.location_service.controller;

import com.easyride.location_service.dto.ApiResponse; // Ensure this path is correct
import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/location") // Keep existing base path or change as needed
public class LocationController {

    private static final Logger log = LoggerFactory.getLogger(LocationController.class);
    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // Existing endpoint for geocoding
    @GetMapping("/info")
    public ApiResponse<LocationResponse> getLocationInfo(@RequestParam double lat, @RequestParam double lon) {
        log.info("Received request for geocoding: lat={}, lon={}", lat, lon);
        LocationResponse response = locationService.getLocationInfo(lat, lon);
        if (response != null) {
            return ApiResponse.success(response);
        } else {
            return ApiResponse.error(500, "无法获取地理编码信息");
        }
    }

    // New endpoint for driver to update their location
    @PostMapping("/driver/{driverId}")
    public ApiResponse<String> updateDriverLocation(@PathVariable Long driverId,
                                                    @Valid @RequestBody DriverLocationUpdateDto locationUpdateDto) {
        log.info("Driver {} updating location: {}", driverId, locationUpdateDto);
        locationService.updateDriverLocation(driverId, locationUpdateDto);
        return ApiResponse.successMessage("司机位置更新成功");
    }

    // New endpoint to get a driver's current location
    @GetMapping("/driver/{driverId}")
    public ApiResponse<LocationDataDto> getDriverLocation(@PathVariable Long driverId) {
        log.info("Fetching location for driver {}", driverId);
        LocationDataDto locationData = locationService.getDriverLocation(driverId);
        return ApiResponse.success(locationData);
    }

    // New endpoint to get trip path for an order
    @GetMapping("/trip/{orderId}/path")
    public ApiResponse<List<LocationDataDto>> getOrderTripPath(@PathVariable Long orderId) {
        log.info("Fetching trip path for order {}", orderId);
        List<LocationDataDto> tripPath = locationService.getTripPath(orderId);
        return ApiResponse.success(tripPath);
    }
}