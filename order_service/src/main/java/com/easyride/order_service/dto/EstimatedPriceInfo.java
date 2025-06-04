package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimatedPriceInfo {
    private double estimatedCost;
    private double estimatedDistance; // km
    private double estimatedDuration; // minutes
    private String currency;
    private String priceBreakdown; // e.g., JSON string or structured object with base_fare, per_km, per_min, surge_multiplier
}