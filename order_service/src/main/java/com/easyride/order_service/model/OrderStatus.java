package com.easyride.order_service.model;

public enum OrderStatus {
    PENDING_PAYMENT,    // If pre-authorization or payment is needed before matching
    PENDING_MATCH,      // Order created, waiting for Matching Service to assign a driver
    SCHEDULED,          // For pre-booked rides, not yet sent for matching
    DRIVER_ASSIGNED,    // Matching Service has assigned a driver
    ACCEPTED,           // Driver explicitly accepted (if your flow requires this after assignment)
    DRIVER_EN_ROUTE,    // Driver is on the way to pickup
    ARRIVED,            // Driver has arrived at pickup location
    IN_PROGRESS,        // Trip started
    COMPLETED,          // Trip finished, pending final payment settlement
    PAYMENT_SETTLED,    // Final payment successful
    CANCELED,
    FAILED              // Order failed for some reason (e.g., no drivers, payment failed)
}
