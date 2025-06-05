package com.easyride.matching_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableOrderDto {
    private Long orderId;
    private String startAddress;
    private String endAddress;
    private String serviceType;
    private String vehicleTypeRequired;
    private Double estimatedFare;
    private LocalDateTime orderTime;
    private Double distanceToPickupKm; // Calculated for the requesting driver
}