package com.easyride.matching_service.model;

public enum GrabbableOrderStatus {
    PENDING_GRAB, // Available for drivers
    GRABBED,      // A driver has claimed it
    EXPIRED,      // No one grabbed it in time, may need auto-match
    ASSIGNED      // Confirmed assignment to the grabbing driver
}