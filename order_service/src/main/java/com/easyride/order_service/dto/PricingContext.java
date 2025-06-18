package com.easyride.order_service.dto;

import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.VehicleType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PricingContext {
    private LocationDto startLocation;
    private LocationDto endLocation;
    private VehicleType vehicleType;
    private ServiceType serviceType;
    private LocalDateTime scheduledTime; // For potential peak hour charges
    private Double actualDistanceKm; // For final calculation
    private Double actualDurationMinutes; // For final calculation
}