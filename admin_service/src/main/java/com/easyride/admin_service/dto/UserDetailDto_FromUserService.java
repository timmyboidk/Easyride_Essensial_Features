package com.easyride.admin_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

// This DTO represents the detailed information of a single user,
// as returned by the user-service.
@Data
public class UserDetailDto_FromUserService {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String role; // e.g., "PASSENGER", "DRIVER"
    private boolean enabled;
    private LocalDateTime createdAt;
    // Add other fields like address, real name, etc.
}