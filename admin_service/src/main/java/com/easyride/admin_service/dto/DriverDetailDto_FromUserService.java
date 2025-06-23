package com.easyride.admin_service.dto;

import lombok.Data;

/**
 * Represents the detailed driver information returned by the User Service
 * after an update operation.
 */
@Data
public class DriverDetailDto_FromUserService {
    private Long driverId;
    private String username;
    private String approvalStatus;
    private boolean enabled;
    // Add any other relevant driver details
}