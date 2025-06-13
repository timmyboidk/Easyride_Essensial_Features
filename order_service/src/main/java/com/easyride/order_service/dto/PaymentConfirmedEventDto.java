package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEventDto {
    private Long orderId;
    private Double finalAmount;
    private String paymentTransactionId;
}