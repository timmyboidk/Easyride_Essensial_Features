package com.easyride.matching_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatus {
    @Id
    private Long driverId; // Corresponds to User ID from User Service

    private double currentLatitude;
    private double currentLongitude;
    private boolean available;
    private double rating; // Synced or calculated
    private String vehicleType; // e.g., SEDAN, SUV (synced from User Service)

    private LocalDateTime lastLocationUpdateTime;
    private LocalDateTime lastStatusUpdateTime;
    private LocalDateTime onlineSince; // Track when driver came online

    // New fields for preferences and advanced matching
    private String currentCity; // Can be derived from lat/lon or set by driver
    // @ElementCollection(fetch = FetchType.EAGER) // If storing simple string tags
    // @CollectionTable(name = "driver_preferred_passenger_types", joinColumns =
    // @JoinColumn(name = "driver_id"))
    // @Column(name = "passenger_type")
    // private Set<String> preferredPassengerTypes; // e.g. "NO_PETS", "BUSINESS"

    // @ElementCollection(fetch = FetchType.EAGER)
    // @CollectionTable(name = "driver_service_areas", joinColumns =
    // @JoinColumn(name = "driver_id"))
    // @Column(name = "service_area_zipcode") // Or more complex geofence ID
    // private Set<String> serviceAreas;

    private int currentPassengersInCar; // For carpooling
    private int maxCapacityForCarpool; // Vehicle's capacity for carpooling

    @Version
    private Long version;
}
