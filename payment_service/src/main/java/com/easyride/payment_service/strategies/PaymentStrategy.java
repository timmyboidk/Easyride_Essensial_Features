package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;

public interface PaymentStrategy {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequest);

    PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency);

    boolean supports(String paymentMethodType);
}