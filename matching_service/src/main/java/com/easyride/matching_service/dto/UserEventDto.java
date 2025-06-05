package com.easyride.matching_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDto { // Consumed from user-service
    private Long userId;
    private String username;
    private String email;
    private String role; // "DRIVER", "PASSENGER"
    private String eventType; // "USER_CREATED", "DRIVER_APPLICATION_SUBMITTED", "DRIVER_APPROVED", "USER_UPDATED"

    // Driver specific fields if eventType indicates driver context
    private String vehicleType; // e.g. SEDAN, SUV. This should come from User Service.
    private String vehicleInfo; // Full vehicle details
    private Double initialRating; // e.g. default rating for new drivers
}