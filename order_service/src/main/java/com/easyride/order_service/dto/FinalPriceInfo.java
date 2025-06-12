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
    private Long finalPrice;
    private double distance;
    private long durationMinutes;
    private Long baseFare;
    private Long distanceCost;
    private Long timeCost;
}