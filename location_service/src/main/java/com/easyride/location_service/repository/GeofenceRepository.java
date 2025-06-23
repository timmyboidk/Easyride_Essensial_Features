package com.easyride.location_service.repository;

import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.model.GeofenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    List<Geofence> findByIsActiveTrue();

    List<Geofence> findByTypeAndIsActiveTrue(GeofenceType type);

    Optional<Geofence> findByName(String name);
}