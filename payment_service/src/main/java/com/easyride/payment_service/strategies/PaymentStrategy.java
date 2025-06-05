package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto; // You'll need a common response structure
import com.easyride.payment_service.model.PassengerPaymentMethod; // For charging stored methods

public interface PaymentStrategy {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails);
    PaymentResponseDto refundPayment(String transactionId, Double amount, String currency); // transactionId from original payment
    boolean supports(String paymentMethodType); // e.g., "CREDIT_CARD_STRIPE", "PAYPAL"
}