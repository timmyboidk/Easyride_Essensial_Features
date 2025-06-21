package com.easyride.payment_service.strategies;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("STRIPE_CREDIT_CARD")
public class StripeStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(StripeStrategy.class);

    @Value("${stripe.api-key:}") // Provide a default empty value
    private String stripeSecretKey;

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        log.info("Processing payment with Stripe for order ID: {}", paymentRequest.getOrderId());
        if (paymentMethodDetails == null || paymentMethodDetails.getPaymentGatewayToken() == null) {
            log.error("Stripe payment requires a stored payment method token.");
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.FAILED)
                    .message("支付方式信息不完整")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        try {
            log.warn("Stripe PaymentIntent creation logic is a SKELETON. Actual Stripe SDK calls are commented out.");
            String mockTransactionId = "pi_mock_" + System.currentTimeMillis();
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.COMPLETED)
                    .transactionId(mockTransactionId)
                    .amount(paymentRequest.getAmount())
                    .currency(paymentRequest.getCurrency())
                    .message("支付成功 (Stripe Mock)")
                    .paymentGatewayUsed("STRIPE")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Stripe API error during payment for order {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
            return PaymentResponseDto.builder()
                    .orderId(paymentRequest.getOrderId().toString())
                    .status(PaymentStatus.FAILED)
                    .message("Stripe支付失败: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public PaymentResponseDto refundPayment(String transactionId, Integer amount, String currency) {
        log.info("Processing Stripe refund for transaction ID: {}, Amount: {}", transactionId, amount);
        try {
            log.warn("Stripe Refund creation logic is a SKELETON. Actual Stripe SDK calls are commented out.");
            return PaymentResponseDto.builder()
                    .transactionId(transactionId)
                    .status(PaymentStatus.REFUNDED)
                    .amount(amount)
                    .currency(currency)
                    .message("退款成功 (Stripe Mock)")
                    .paymentGatewayUsed("STRIPE")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Stripe API error during refund for transaction {}: {}", transactionId, e.getMessage(), e);
            return PaymentResponseDto.builder()
                    .transactionId(transactionId)
                    .status(PaymentStatus.REFUND_FAILED)
                    .message("Stripe退款失败: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    public boolean supports(String paymentMethodType) {
        return "CREDIT_CARD".equalsIgnoreCase(paymentMethodType) || "DEBIT_CARD".equalsIgnoreCase(paymentMethodType);
    }
}