package com.easyride.payment_service.dto;

import com.easyride.payment_service.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private Double amount;
    private String currency;
    private String transactionId; // Gateway's transaction ID
    private String message;
    private LocalDateTime timestamp;
    private String paymentGatewayUsed; // e.g., "STRIPE", "PAYPAL"
    // Fields for redirect URL if 3D Secure or PayPal redirect is needed
    private String redirectUrl;
    private boolean requiresAction;
}

