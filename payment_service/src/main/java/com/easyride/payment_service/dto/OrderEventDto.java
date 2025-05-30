package com.easyride.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEventDto {
    private Long orderId;
    private String eventType; // ORDER_CREATED, ORDER_COMPLETED, etc.
    private Long passengerId;
    private Long driverId;
    // 其他与订单相关的字段
}
