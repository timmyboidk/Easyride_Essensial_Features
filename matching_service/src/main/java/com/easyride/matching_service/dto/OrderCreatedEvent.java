package com.easyride.matching_service.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

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
    private Double endLatitude;
    private Double endLongitude;
    private String vehicleType;
    private String serviceType;
    private String paymentMethod;
    private Double estimatedCost;
    private LocalDateTime createdTime; // Note: Listener uses getOrderTime, changed to match
    private LocalDateTime orderTime; // Added to match listener
    private LocalDateTime scheduledTime;
    private Double passengerRating;
    private Set<String> preferredDriverTags;
    private Long preferredDriverId;
}
