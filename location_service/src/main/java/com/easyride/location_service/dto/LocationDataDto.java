package com.easyride.location_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDataDto {
    private Long entityId; // Driver ID or other entity
    private Double latitude;
    private Double longitude;
    private Instant lastUpdateTime;
    private Long orderId; // If relevant
    private String formattedAddress; // Optional: If reverse geocoding is done
}