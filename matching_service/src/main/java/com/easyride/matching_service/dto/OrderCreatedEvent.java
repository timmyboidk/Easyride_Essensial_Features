package com.easyride.matching_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderCreatedEvent {
    private Long orderId;
    private Long passengerId;
    private Double startLatitude;
    private Double startLongitude;
    private String vehicleType;
    private String serviceType;
    private String paymentMethod;
    private Double estimatedCost;
    private LocalDateTime createdTime;
}
