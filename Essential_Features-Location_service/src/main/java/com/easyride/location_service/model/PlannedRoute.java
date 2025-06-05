package com.easyride.location_service.model;

import com.easyride.location_service.dto.LocationDataDto; // Or a simpler LatLng DTO
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlannedRoute {
    private Long orderId;
    private Long driverId;
    private List<LocationDataDto> waypoints; // Ordered list of LatLng points defining the route
}