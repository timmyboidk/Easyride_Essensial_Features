package com.easyride.notification_service.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map; // For generic additional data

@Data
public class ConsumedOrderEventDto { // Consumed from Order Service
    private Long orderId;
    private Long passengerId;
    private Long driverId;
    private String eventType; // e.g., "ORDER_ACCEPTED", "DRIVER_ARRIVED", "ORDER_COMPLETED", "ORDER_CANCELLED"
    private LocalDateTime timestamp;
    private String defaultMessage; // A simple message from publishing service
    private Map<String, Object> additionalData; // For template model (e.g., driverName, eta, rideAmount)
    // Fields for user preferences if available
    private String passengerLocale; // e.g. "en_US"
    private String passengerPhoneNumber;
    private String passengerEmail;
    private String passengerApnsToken;
    private String passengerFcmToken;
    // Similar fields for driver if notification is for driver
}