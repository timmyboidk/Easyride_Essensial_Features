package com.easyride.location_service.dto;

import com.easyride.location_service.model.PlannedRoute;
import lombok.Data;

@Data
public class OrderStartedEventDto {
    private Long orderId;
    private PlannedRoute plannedRoute;
}