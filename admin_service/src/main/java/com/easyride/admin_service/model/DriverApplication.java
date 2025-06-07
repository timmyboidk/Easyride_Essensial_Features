package com.easyride.admin_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_applications")
@Data
@NoArgsConstructor
public class DriverApplication {
    @Id
    private Long driverId; // Corresponds to User ID from User Service

    private String username; // From event
    private String driverLicenseNumber; // From event
    // Store URLs or paths to documents if User Service provided them
    // private String driverLicenseDocumentUrl;
    // private String vehicleDocumentUrl;

    @Enumerated(EnumType.STRING)
    private DriverApplicationStatus status;

    private LocalDateTime applicationTime; // From event or when this record created
    private LocalDateTime reviewTime;
    private Long reviewedByAdminId;
    private String adminNotes;

    public DriverApplication(Long driverId, String username, String driverLicenseNumber, LocalDateTime applicationTime) {
        this.driverId = driverId;
        this.username = username;
        this.driverLicenseNumber = driverLicenseNumber;
        this.applicationTime = applicationTime;
        this.status = DriverApplicationStatus.PENDING_REVIEW;
    }
}