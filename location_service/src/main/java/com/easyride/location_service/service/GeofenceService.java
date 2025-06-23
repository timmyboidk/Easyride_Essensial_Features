package com.easyride.location_service.service;

import com.easyride.location_service.model.Geofence;

import java.util.List;

public interface GeofenceService {
    Geofence createGeofence(Geofence geofence);

    Geofence updateGeofence(Long id, Geofence geofenceDetails);

    void deleteGeofence(Long id);

    List<Geofence> getAllActiveGeofences();

    List<Geofence> getActiveGeofencesByType(String type);

    boolean isPointInGeofence(double latitude, double longitude, Long geofenceId);

    List<Geofence> findGeofencesContainingPoint(double latitude, double longitude, String typeFilter); // typeFilter can be null
}