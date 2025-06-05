package com.easyride.location_service.model;

public enum GeofenceType {
    SERVICE_AREA,       // General operational area
    RESTRICTED_ZONE,    // Area where service is not allowed or limited
    SURGE_PRICING_ZONE, // Area with special pricing
    PICKUP_HOTSPOT,     // Designated pickup locations
    NO_GO_ZONE          // Forbidden areas
}