package com.easyride.location_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestEvent {
    private String correlationId;
    private double latitude;
    private double longitude;
}