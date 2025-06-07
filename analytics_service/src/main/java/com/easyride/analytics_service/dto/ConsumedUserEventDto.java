package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumedUserEventDto { // Matches UserEventDto from User Service
    private Long userId;
    private String username;
    private String email;
    private String role; // "PASSENGER", "DRIVER"
    private String eventType; // e.g., "USER_CREATED", "DRIVER_APPLICATION_APPROVED"
    private LocalDateTime timestamp; // Add timestamp if User Service sends it
}