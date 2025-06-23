package com.easyride.location_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseEvent {
    private String correlationId;
    private String formattedAddress;
    private String placeId;
}