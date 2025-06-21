package com.easyride.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEventDto {
    private Long paymentId;
    private Long orderId;
    private Long passengerId;
    private String eventType; // e.g., "PAYMENT_COMPLETED", "PAYMENT_REFUNDED"
    private String status; // COMPLETED, FAILED, etc.
    private Integer amount;
    private String currency;
    private LocalDateTime timestamp;
}

