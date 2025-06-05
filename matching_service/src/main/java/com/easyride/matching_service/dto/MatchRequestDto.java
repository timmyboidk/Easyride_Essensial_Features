package com.easyride.matching_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set; // For passenger preferences

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestDto {

    private Long orderId;
    private Long passengerId;
    private Double startLatitude;
    private Double startLongitude;
    private String startAddressFormatted; // New from geocoding
    private double endLatitude;
    private double endLongitude;
    private String endAddressFormatted; // New from geocoding
    private String vehicleTypeRequired; // e.g., "SEDAN"
    private String serviceType;       // e.g., "NORMAL", "CARPOOL"
    private Double estimatedCost;
    private LocalDateTime scheduledTime;
    private LocalDateTime orderTime;
    // Passenger preferences
    private Double passengerRating; // To be seen by driver
    private Set<String> preferredDriverTags; // e.g., "QUIET_RIDE", "CHATTY"
    private Long preferredDriverId; // If passenger favorited a driver
}
