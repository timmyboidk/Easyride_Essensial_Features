package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PassengerPaymentMethod;

public interface PaymentStrategy {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails);
    PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency); // Changed from Double to Integer
    boolean supports(String paymentMethodType);
}