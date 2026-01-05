package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PayPalStrategy implements PaymentStrategy {

    @Value("${payment-gateway.paypal.client-id}")
    private String clientId;

    @Value("${payment-gateway.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment-gateway.paypal.mode}")
    private String mode;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        log.info("Processing PayPal payment for amount: {}", request.getAmount());
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus(PaymentStatus.COMPLETED);
        response.setTransactionId("PAYPAL_MOCK_" + System.currentTimeMillis());
        response.setMessage("PayPal payment mocked success");
        return response;
    }

    @Override
    public PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency) {
        log.info("Refunding PayPal payment: {}", transactionId);
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus(PaymentStatus.REFUNDED);
        response.setTransactionId("PAYPAL_REFUND_" + System.currentTimeMillis());
        response.setMessage("PayPal refund mocked success");
        return response;
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return "PAYPAL".equalsIgnoreCase(paymentMethodType);
    }
}