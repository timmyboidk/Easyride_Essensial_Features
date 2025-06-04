package com.easyride.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentSettledEvent {
    private Long orderId;
    private Double finalAmount; // Use Double or BigDecimal
    private String paymentMethod;
    private LocalDateTime paymentTimestamp;
    private Long passengerId;
    private Long driverId; // Optional, but good for context
    // Potentially transaction ID from payment service
    // private String paymentTransactionId;
}