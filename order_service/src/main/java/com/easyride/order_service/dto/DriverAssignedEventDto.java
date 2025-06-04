package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverAssignedEventDto {
    private Long orderId;
    private Long driverId;
    private String driverName; // Optional: from Matching Service's cache of driver data
    private String vehiclePlate; // Optional
    private String vehicleModel; // Optional
    private Double driverRating; // Optional
    private LocalDateTime estimatedArrivalTime; // ETA of driver to pickup
}