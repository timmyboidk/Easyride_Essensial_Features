package com.easyride.order_service.model;

public enum ServiceType {
    NORMAL,         // Standard ride
    EXPRESS,        // Faster matching, potentially higher price
    LUXURY,         // Premium vehicles
    AIRPORT_TRANSFER, // Specific for airport
    LONG_DISTANCE,  // For inter-city or long trips
    CHARTER_HOURLY, // Charter by hour
    CHARTER_DAILY,  // Charter by day
    CARPOOL         // New
}
