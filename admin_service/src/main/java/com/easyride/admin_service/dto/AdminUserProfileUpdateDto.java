package com.easyride.admin_service.dto;

import lombok.Data;

// This DTO carries the data for an admin updating a user's profile.
@Data
public class AdminUserProfileUpdateDto {
    private String email;
    private String phoneNumber;
    // Add any other fields an admin is allowed to update
}