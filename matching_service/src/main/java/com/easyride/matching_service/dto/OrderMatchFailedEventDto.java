package com.easyride.matching_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchFailedEventDto {
    private Long orderId;
    private String reason; // e.g., "NO_DRIVERS_AVAILABLE", "NO_SUITABLE_DRIVERS"
}