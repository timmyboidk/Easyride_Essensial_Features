package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderStatusChangedEventDto {
    private Long orderId;
    private Long userId;
    private String userRole;
    private String newStatus;
    private Long driverId;
    private LocalDateTime timestamp;
}