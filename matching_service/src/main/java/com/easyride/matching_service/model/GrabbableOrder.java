package com.easyride.matching_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "grabbable_orders")
@Data
@NoArgsConstructor
public class GrabbableOrder { // Represents an order available for drivers to grab
    @Id
    private Long orderId; // Same as the actual order ID from Order Service

    private double startLatitude;
    private double startLongitude;
    private String startAddressFormatted;
    private double endLatitude;
    private double endLongitude;
    private String endAddressFormatted;
    private String vehicleTypeRequired;
    private String serviceType;
    private Double estimatedFare;
    private LocalDateTime orderTime;
    private LocalDateTime expiryTime; // Time after which this grabbable order might be auto-matched or removed

    @Enumerated(EnumType.STRING)
    private GrabbableOrderStatus status; // PENDING_GRAB, GRABBED, EXPIRED

    private Long grabbingDriverId; // Who grabbed it

    public GrabbableOrder(Long orderId, double startLatitude, double startLongitude, String startAddressFormatted, double endLatitude, double endLongitude, String endAddressFormatted, String vehicleTypeRequired, String serviceType, Double estimatedFare, LocalDateTime orderTime, LocalDateTime expiryTime) {
        this.orderId = orderId;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.startAddressFormatted = startAddressFormatted;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.endAddressFormatted = endAddressFormatted;
        this.vehicleTypeRequired = vehicleTypeRequired;
        this.serviceType = serviceType;
        this.estimatedFare = estimatedFare;
        this.orderTime = orderTime;
        this.status = GrabbableOrderStatus.PENDING_GRAB;
        this.expiryTime = expiryTime;
    }
}