package com.easyride.notification_service.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private Long passengerId;
    private String origin;
    private String destination;
    private String serviceType;
    private LocalDateTime createdTime;
}
