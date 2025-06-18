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
    private Long finalCost;
    private Double actualDistance;
    private Long actualDuration;
    private Long baseFare;
    private Long distanceCost;
    private Long timeCost;
}