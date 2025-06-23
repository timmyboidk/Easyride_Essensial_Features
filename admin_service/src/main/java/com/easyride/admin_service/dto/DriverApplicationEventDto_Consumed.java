package com.easyride.admin_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents the driver application event consumed from a message queue.
 * This DTO mirrors the data sent by the User Service when a driver applies.
 */
@Data
public class DriverApplicationEventDto_Consumed {

    private Long driverId;
    private String username;
    private String driverLicenseNumber;
    private LocalDateTime applicationTime;
    // You could add other fields that the User Service might send,
    // like URLs to uploaded documents.
    // private String driverLicenseDocumentUrl;
}