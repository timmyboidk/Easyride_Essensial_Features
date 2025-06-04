package com.easyride.order_service.dto;

import com.easyride.order_service.model.PaymentMethod;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long passengerId;
    private double startLatitude;
    private double startLongitude;
    private double endLatitude;
    private double endLongitude;
    private VehicleType vehicleType;
    private ServiceType serviceType;
    private PaymentMethod paymentMethod;
    private Double estimatedCost;
    private LocalDateTime scheduledTime; // Nullable, for scheduled rides
    private LocalDateTime orderTime;
    private String passengerName; // Optional: for context in other services
    // Add any other info Matching Service might need, e.g., passenger rating, preferences
}