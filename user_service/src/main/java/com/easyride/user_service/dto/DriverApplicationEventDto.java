package com.easyride.user_service.dto; // Re-index

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverApplicationEventDto {
    private Long driverId;
    private String username;
    // Add other relevant info for admin review
    private String driverLicenseNumber;
    // private String driverLicenseDocumentUrl; // URL if files are stored and
    // accessible
    // private String vehicleDocumentUrl;
    private LocalDateTime applicationTime;

    public DriverApplicationEventDto(Long driverId, String username, String driverLicenseNumber) {
        this.driverId = driverId;
        this.username = username;
        this.driverLicenseNumber = driverLicenseNumber;
        this.applicationTime = LocalDateTime.now();
    }
}