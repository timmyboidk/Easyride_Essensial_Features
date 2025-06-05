package com.easyride.matching_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverAssignedEventDto { // Published by Matching Service
    private Long orderId;
    private Long driverId;
    private String driverName;
    private String vehiclePlate;
    private String vehicleModel; // Or just vehicleType string
    private Double driverRating;
    private LocalDateTime estimatedDriverArrivalTimeToPickup;
}