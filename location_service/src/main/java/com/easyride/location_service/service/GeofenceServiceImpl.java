package com.easyride.location_service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.location_service.model.Geofence;
import com.easyride.location_service.model.GeofenceType;
import com.easyride.location_service.repository.GeofenceMapper;
import com.easyride.location_service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeofenceServiceImpl implements GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceServiceImpl.class);
    private final GeofenceMapper geofenceMapper;
    private final ObjectMapper objectMapper; // For parsing polygon JSON

    public GeofenceServiceImpl(GeofenceMapper geofenceMapper, ObjectMapper objectMapper) {
        this.geofenceMapper = geofenceMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Geofence createGeofence(Geofence geofence) {
        log.info("Creating new geofence: {}", geofence.getName());
        geofenceMapper.insert(geofence);
        return geofence;
    }
    // ... Implement updateGeofence, deleteGeofence, getAllActiveGeofences,
    // getActiveGeofencesByType ...

    @Override
    @Transactional
    public Geofence updateGeofence(Long id, Geofence geofenceDetails) {
        log.info("Updating geofence with id: {}", id);
        Geofence existingGeofence = geofenceMapper.selectById(id);
        if (existingGeofence == null) {
            throw new ResourceNotFoundException("Geofence not found with id: " + id);
        }

        geofenceDetails.setId(id);
        geofenceMapper.updateById(geofenceDetails);
        return geofenceDetails;
    }

    @Override
    @Transactional
    public void deleteGeofence(Long id) {
        log.info("Deleting geofence with id: {}", id);
        if (geofenceMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("Geofence not found with id: " + id);
        }
        geofenceMapper.deleteById(id);
    }

    @Override
    public List<Geofence> getAllActiveGeofences() {
        return geofenceMapper.selectList(new LambdaQueryWrapper<Geofence>()
                .eq(Geofence::isActive, true));
    }

    @Override
    public List<Geofence> getActiveGeofencesByType(String typeString) {
        try {
            GeofenceType type = GeofenceType.valueOf(typeString.toUpperCase());
            return geofenceMapper.selectList(new LambdaQueryWrapper<Geofence>()
                    .eq(Geofence::getType, type)
                    .eq(Geofence::isActive, true));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid geofence type string: {}", typeString);
            return List.of();
        }
    }

    @Override
    public boolean isPointInGeofence(double latitude, double longitude, Long geofenceId) {
        Geofence geofence = geofenceMapper.selectById(geofenceId);
        if (geofence == null) {
            throw new ResourceNotFoundException("Geofence not found with id: " + geofenceId);
        }
        if (!geofence.isActive())
            return false;
        return isPointInPolygon(latitude, longitude, parsePolygon(geofence.getPolygonCoordinatesJson()));
    }

    @Override
    public List<Geofence> findGeofencesContainingPoint(double latitude, double longitude, String typeFilter) {
        List<Geofence> candidates;
        LambdaQueryWrapper<Geofence> queryWrapper = new LambdaQueryWrapper<Geofence>().eq(Geofence::isActive, true);
        if (typeFilter != null && !typeFilter.isBlank()) {
            try {
                GeofenceType type = GeofenceType.valueOf(typeFilter.toUpperCase());
                queryWrapper.eq(Geofence::getType, type);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid geofence type filter '{}', finding in all active geofences.", typeFilter);
            }
        }
        candidates = geofenceMapper.selectList(queryWrapper);

        return candidates.stream()
                .filter(gf -> isPointInPolygon(latitude, longitude, parsePolygon(gf.getPolygonCoordinatesJson())))
                .collect(Collectors.toList());
    }

    private List<Geofence.Coordinate> parsePolygon(String polygonJson) {
        if (polygonJson == null || polygonJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<List<Double>> pointsList = objectMapper.readValue(polygonJson,
                    new TypeReference<List<List<Double>>>() {
                    });
            return pointsList.stream()
                    .filter(p -> p.size() == 2)
                    .map(p -> new Geofence.Coordinate(p.get(0), p.get(1)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error parsing geofence polygon JSON: {}", polygonJson, e);
            return new ArrayList<>();
        }
    }

    // Ray Casting Algorithm for Point in Polygon
    private boolean isPointInPolygon(double testLat, double testLon, List<Geofence.Coordinate> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        int i, j;
        boolean result = false;
        for (i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Geofence.Coordinate pi = polygon.get(i);
            Geofence.Coordinate pj = polygon.get(j);
            if (((pi.getLatitude() > testLat) != (pj.getLatitude() > testLat)) &&
                    (testLon < (pj.getLongitude() - pi.getLongitude()) * (testLat - pi.getLatitude())
                            / (pj.getLatitude() - pi.getLatitude()) + pi.getLongitude())) {
                result = !result;
            }
        }
        return result;
    }
}