package com.easyride.matching_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequestDto {

    private Long orderId;
    private Long passengerId;
    private Double startLatitude;
    private Double startLongitude;
    private String vehicleType; // 订单需求
    private String serviceType; // NORMAL, EXPRESS, LUXURY 等
    private Double estimatedCost;
    private String paymentMethod; // CREDIT_CARD, CASH 等
}
