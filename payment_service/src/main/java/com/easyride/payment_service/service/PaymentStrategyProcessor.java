package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.strategies.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyProcessor {

    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentStrategyProcessor(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(s -> s.getClass().getSimpleName().replace("Strategy", "").toUpperCase(),
                        Function.identity()));
        // Map keys example: PAYPAL, STRIPE
    }

    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest) {
        String strategyKey = paymentRequest.getPaymentMethod();
        if (strategyKey == null) {
            throw new PaymentServiceException("Payment method is required");
        }

        PaymentStrategy strategy = getStrategy(strategyKey);
        return strategy.processPayment(paymentRequest);
    }

    public PaymentResponseDto refundPayment(String originalTransactionId, String paymentGatewayUsed, Integer amount,
            String currency) {
        PaymentStrategy strategy = getStrategy(paymentGatewayUsed);
        return strategy.refundPayment(originalTransactionId, amount, currency);
    }

    private PaymentStrategy getStrategy(String strategyKey) {
        // Handle cases like "PAYPAL_CHECKOUT" -> "PAYPAL" mapping if needed, or assume
        // exact match
        // Or cleaner: Strategies should define their identifier.
        // For now, let's assume keys match simple names or we iterate.
        // Actually, easiest is to allow strategies to self-identify or use bean names.
        // But the previous impl logic had "determineStrategyKey".

        // Let's iterate and find matching strategy if map lookup fails, or better,
        // stick to a convention.
        // com.easyride.payment_service.strategies.PayPalStrategy -> PAYPAL
        // com.easyride.payment_service.strategies.StripeStrategy -> STRIPE

        PaymentStrategy strategy = strategyMap.get(strategyKey.toUpperCase());
        if (strategy == null) {
            // Fallback or detailed error
            // Maybe strategyKey is "CREDIT_CARD" but we use Stripe?
            // For simplify, we assume the DTO passes "STRIPE" or "PAYPAL" as method for
            // now,
            // OR we map methods to gateways.
            // Given the requirements, let's assume the passed method IS the gateway name
            // for simplicity in this task.
            throw new PaymentServiceException("No payment strategy found for: " + strategyKey);
        }
        return strategy;
    }
}