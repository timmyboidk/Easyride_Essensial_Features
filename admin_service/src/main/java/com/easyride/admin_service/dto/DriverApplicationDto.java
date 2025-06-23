package com.easyride.admin_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for exposing driver application details through the Admin API.
 */
@Data
public class DriverApplicationDto {

    private Long driverId;
    private String username;
    private String driverLicenseNumber;
    private String status; // The status is a String in the DTO (e.g., "PENDING_REVIEW")
    private LocalDateTime applicationTime;
    private String adminNotes;
    // Add other fields from the DriverApplication entity as needed
}