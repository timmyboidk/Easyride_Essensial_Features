package com.easyride.matching_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverStatusUpdateDto {
    // Required fields for any status update
    @NotNull(message = "Availability status cannot be null")
    private Boolean available; // true for online/available, false for offline/busy

    // Optional fields, only provided if they are being updated
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentCity; // Driver might update this manually or it's derived

    // Vehicle related updates should ideally come from User Service events if they require re-approval.
    // But if driver can temporarily switch vehicle type IF platform allows multiple registered vehicles:
    // private String activeVehicleType;
}