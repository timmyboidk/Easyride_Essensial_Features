package com.easyride.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEventDto {
    private Long paymentId;
    private Long orderId;
    private Integer amount;
    private String status; // COMPLETED, FAILED, etc.
    private String currency;
    private String paymentMethod; // PAYPAL, CREDIT_CARD, etc.
}

