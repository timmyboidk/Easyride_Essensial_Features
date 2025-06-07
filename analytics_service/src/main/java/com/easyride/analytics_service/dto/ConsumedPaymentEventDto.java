package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumedPaymentEventDto { // Matches PaymentEventDto/PaymentFailedEventDto from Payment Service
    private String paymentId; // Internal Payment ID from PaymentService
    private Long orderId;
    private Long passengerId;
    // private Long driverId; // Add if PaymentService includes this
    private String eventType; // e.g., "PAYMENT_COMPLETED", "PAYMENT_FAILED", "PAYMENT_REFUNDED"
    private String paymentStatus; // From PaymentStatus enum in PaymentService
    private Double amount;
    private String currency;
    private String failureReason; // For PAYMENT_FAILED
    private LocalDateTime timestamp;
}