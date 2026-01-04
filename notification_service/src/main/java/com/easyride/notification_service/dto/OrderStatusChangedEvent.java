package com.easyride.notification_service.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class OrderStatusChangedEvent implements Serializable {
    private Long orderId;
    private String oldStatus;
    private String newStatus;
    private Long driverId;
    private Long passengerId;
    private LocalDateTime timestamp;
    // Additional fields for notification context
    private String passengerPhoneNumber;
    private String passengerEmail;
    private String driverPhoneNumber;
    private String driverEmail;
    private Map<String, Object> additionalData;
}
