package com.easyride.location_service.service;

import com.easyride.location_service.dto.DriverLocationUpdateDto;
import com.easyride.location_service.dto.LocationDataDto;
import com.easyride.location_service.model.LocationResponse; // Existing for geocoding

public interface LocationService {
    // Existing geocoding method
    LocationResponse getLocationInfo(double latitude, double longitude);

    // New methods for real-time tracking
    void updateDriverLocation(Long driverId, DriverLocationUpdateDto locationUpdateDto);
    LocationDataDto getDriverLocation(Long driverId);

    // Method to store trip path (could be internal or exposed)
    void recordTripLocation(Long orderId, Long driverId, double latitude, double longitude, Instant timestamp);
    List<LocationDataDto> getTripPath(Long orderId); // Example
}