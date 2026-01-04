package com.easyride.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private Long passengerId;
    private String pickupAddress;
    private String dropoffAddress;
    private String serviceType;
    private BigDecimal estimatedFare;
    private LocalDateTime createdTime;
}
