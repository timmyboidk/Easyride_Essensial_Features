package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(name = "payment.method", havingValue = "paypal")
public class PayPalStrategy implements PaymentStrategy {

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        // TODO: Implement PayPal SDK integration
        // This is a mock response.
        return PaymentResponseDto.builder()
                .orderId(paymentRequest.getOrderId().toString())
                .status(PaymentStatus.COMPLETED)
                .transactionId("paypal_mock_" + System.currentTimeMillis())
                .message("Payment processed successfully by PayPal (Mock)")
                .paymentGatewayUsed("PAYPAL")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency) {
        // TODO: Implement PayPal refund logic
        // This is a mock response.
        return PaymentResponseDto.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.REFUNDED)
                .message("Refund processed by PayPal (Mock)")
                .paymentGatewayUsed("PAYPAL")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return "PAYPAL".equalsIgnoreCase(paymentMethodType);
    }
}