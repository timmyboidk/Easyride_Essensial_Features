package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.strategies.PaymentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentStrategyProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentStrategyProcessor.class);
    private final ApplicationContext applicationContext;
    private final Map<String, PaymentStrategy> strategies; // Cache strategies

    @Autowired
    public PaymentStrategyProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // Eagerly load strategies or load on demand
        this.strategies = applicationContext.getBeansOfType(PaymentStrategy.class);
        log.info("Loaded payment strategies: {}", strategies.keySet());
    }

    public PaymentStrategy getStrategy(String paymentMethodTypeOrBeanName) {
        // paymentMethodTypeOrBeanName could be "PAYPAL", "STRIPE_CREDIT_CARD" etc.
        // This needs a clear mapping from PaymentMethodType enum or DTO field to bean name.

        // Simplistic direct match on bean name for now.
        PaymentStrategy strategy = strategies.get(paymentMethodTypeOrBeanName);

        if (strategy == null) {
            // Fallback: iterate and check supports() if bean names are not direct matches
            Optional<PaymentStrategy> foundStrategy = strategies.values().stream()
                    .filter(s -> s.supports(paymentMethodTypeOrBeanName))
                    .findFirst();
            if (foundStrategy.isPresent()) {
                strategy = foundStrategy.get();
            } else {
                log.error("No payment strategy found for type/bean: {}", paymentMethodTypeOrBeanName);
                throw new PaymentServiceException("不支持的支付方式: " + paymentMethodTypeOrBeanName);
            }
        }
        log.debug("Using strategy {} for type/bean {}", strategy.getClass().getSimpleName(), paymentMethodTypeOrBeanName);
        return strategy;
    }

    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        // Determine strategy based on paymentRequest.getPaymentMethod() or paymentMethodDetails.getMethodType()
        String strategyKey = determineStrategyKey(paymentRequest, paymentMethodDetails);
        PaymentStrategy strategy = getStrategy(strategyKey);
        return strategy.processPayment(paymentRequest, paymentMethodDetails);
    }

    public PaymentResponseDto refundPayment(String originalTransactionId, String paymentGatewayUsed, Double amount, String currency) {
        // paymentGatewayUsed would be like "STRIPE", "PAYPAL" which should map to a strategy bean name or type
        PaymentStrategy strategy = getStrategy(paymentGatewayUsed); // Assume gateway name is strategy key
        return strategy.refundPayment(originalTransactionId, amount, currency);
    }

    private String determineStrategyKey(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        // Logic to map from DTO's paymentMethod string (e.g. "STRIPE_CC", "PAYPAL_ACCOUNT")
        // or PassengerPaymentMethod.methodType to the correct strategy bean name.
        // For now, let's assume paymentRequest.getPaymentMethod() is the key
        // e.g. "CREDIT_CARD" could map to "STRIPE_CREDIT_CARD" if Stripe is your CC processor.
        // This mapping might be complex and configurable.
        if (paymentMethodDetails != null) {
            switch (paymentMethodDetails.getMethodType()) {
                case CREDIT_CARD:
                case DEBIT_CARD:
                    return "STRIPE_CREDIT_CARD"; // Example mapping
                case PAYPAL:
                    return "PAYPAL_ACCOUNT_STRATEGY_BEAN_NAME"; // Replace with actual bean name for PayPal
                // Add other cases
                default:
                    throw new PaymentServiceException("无法确定 " + paymentMethodDetails.getMethodType() + " 的支付策略");
            }
        } else if (paymentRequest.getPaymentMethod() != null) {
            // If it's a new payment attempt without a stored method, the DTO should specify
            // This part is more complex as it might involve a nonce and intent to save
            // For example, if paymentRequest.getPaymentMethod() is "STRIPE_NEW_CARD_NONCE"
            return "STRIPE_CREDIT_CARD"; // Simplified for now
        }
        throw new PaymentServiceException("无法确定支付策略");
    }
}