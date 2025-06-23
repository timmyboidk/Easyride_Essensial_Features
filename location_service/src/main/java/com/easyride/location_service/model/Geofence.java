package com.easyride.location_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; // For polygon points

@Entity
@Table(name = "geofences")
@Data
@NoArgsConstructor
public class Geofence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "Downtown Service Area", "Airport Zone"

    @Enumerated(EnumType.STRING)
    private GeofenceType type; // SERVICE_AREA, RESTRICTED_ZONE, PICKUP_HOTSPOT

    // Storing polygon as a list of points (e.g., JSON string or use PostGIS for real geospatial types)
    // For simplicity with standard JPA, storing as text and parsing.
    @Lob // Large object for potentially many points
    @Column(columnDefinition = "TEXT")
    private String polygonCoordinatesJson; // JSON string of List<List<Double>> like [[lat,lon], [lat,lon], ...]

    private boolean isActive;
    private String description;

    // Transients for parsed polygon - not persisted directly but used in logic
    @Transient
    private transient List<Coordinate> parsedPolygon; // Parsed from JSON string

    // Inner class for coordinates if not using a full DTO
    @Data
    @NoArgsConstructor
    public static class Coordinate {
        double latitude;
        double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}