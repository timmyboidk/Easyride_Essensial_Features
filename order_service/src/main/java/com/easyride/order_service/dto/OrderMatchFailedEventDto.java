package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchFailedEventDto {
    private Long orderId;
    private String reason;
}