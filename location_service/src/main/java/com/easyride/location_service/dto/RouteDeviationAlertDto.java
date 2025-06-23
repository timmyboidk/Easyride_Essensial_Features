package com.easyride.location_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDeviationAlertDto {
    private Long orderId;
    private Long driverId;
    private double currentLatitude;
    private double currentLongitude;
    private double deviationInMeters;
    private Instant alertTime;
    private String message;
}