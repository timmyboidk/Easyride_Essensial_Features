package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalPriceInfo {
    private double finalCost;
    private double actualDistance;
    private double actualDuration;
    private String currency;
    private String priceBreakdown;
    private double cancellationFee; // If applicable
}