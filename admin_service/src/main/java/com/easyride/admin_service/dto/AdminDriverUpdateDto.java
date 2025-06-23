package com.easyride.admin_service.dto;

import lombok.Data;

/**
 * DTO used by the Admin Service to send a request to the User Service
 * to update a driver's status (e.g., approve or reject their application).
 */
@Data
public class AdminDriverUpdateDto {
    private String approvalStatus; // e.g., "APPROVED", "REJECTED"
    private boolean enabled;
    // You could add other fields an admin can update, like vehicle verification status
    // private boolean vehicleVerified;
}