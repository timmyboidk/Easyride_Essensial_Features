package com.easyride.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    private double startLatitude;
    private double startLongitude;
    private double endLatitude;
    private double endLongitude;

    private LocalDateTime orderTime; // Time the order was placed
    private LocalDateTime scheduledTime; // New: Time for the scheduled ride
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualDropOffTime;
    private LocalDateTime driverAssignedTime;
    private LocalDateTime driverEnRouteTime;

    private double estimatedCost;
    private double finalCost; // New

    private double estimatedDistance;
    private double actualDistance; // New

    private double estimatedDuration; // in minutes
    private double actualDuration; // in minutes // New

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String passengerNotes;
    private String cancellationReason; // New
    private Long cancelledByUserId; // New
    @Enumerated(EnumType.STRING)
    private Role cancelledByRole; // New


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy; // passengerId
    private Long updatedBy;

    @Version
    private Long version; // For optimistic locking

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.orderTime == null) this.orderTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

