package com.easyride.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsForPaymentDto { // Sent by Order Service when order is finalized
    private Long orderId;
    private Long passengerId;
    private Long driverId; // Crucial for wallet operations
    private Double finalAmount;
    private String currency;
    private String paymentMethodTypeString; // From Order's PaymentMethod enum
    private Long chosenPaymentMethodId; // If passenger selected a stored payment method in Order flow
    private LocalDateTime orderCompletionTime;
}