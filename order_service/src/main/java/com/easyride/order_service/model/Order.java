package com.easyride.order_service.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@TableName("orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private OrderStatus status;

    @TableField("passenger_id")
    private Long passengerId;

    @TableField("driver_id")
    private Long driverId;

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

    private VehicleType vehicleType;

    private ServiceType serviceType;

    private PaymentMethod paymentMethod;

    private String passengerNotes;
    private String cancellationReason; // New
    private Long cancelledByUserId; // New
    private Role cancelledByRole; // New

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy; // passengerId
    private Long updatedBy;

    @TableField("passenger_count")
    private Integer passengerCount;

    @Version
    private Long version; // For optimistic locking
}
