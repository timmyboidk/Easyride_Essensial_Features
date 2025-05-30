package com.easyride.order_service.dto;

import com.easyride.order_service.model.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long orderId;

    private OrderStatus status;

    private String passengerName;

    private String driverName;

    private double estimatedCost;

    private double estimatedDistance;

    private double estimatedDuration;
}

