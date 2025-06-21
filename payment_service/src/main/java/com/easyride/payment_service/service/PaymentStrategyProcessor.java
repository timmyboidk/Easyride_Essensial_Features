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
    private final Map<String, PaymentStrategy> strategies;

    @Autowired
    public PaymentStrategyProcessor(ApplicationContext applicationContext) {
        this.strategies = applicationContext.getBeansOfType(PaymentStrategy.class);
        log.info("Loaded payment strategies: {}", strategies.keySet());
    }

    public PaymentStrategy getStrategy(String paymentMethodTypeOrBeanName) {
        Optional<PaymentStrategy> foundStrategy = strategies.values().stream()
                .filter(s -> s.supports(paymentMethodTypeOrBeanName))
                .findFirst();

        if (foundStrategy.isPresent()) {
            PaymentStrategy strategy = foundStrategy.get();
            log.debug("Using strategy {} for type/bean {}", strategy.getClass().getSimpleName(), paymentMethodTypeOrBeanName);
            return strategy;
        } else {
            log.error("No payment strategy found for type/bean: {}", paymentMethodTypeOrBeanName);
            throw new PaymentServiceException("不支持的支付方式: " + paymentMethodTypeOrBeanName);
        }
    }

    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        String strategyKey = determineStrategyKey(paymentRequest, paymentMethodDetails);
        PaymentStrategy strategy = getStrategy(strategyKey);
        return strategy.processPayment(paymentRequest, paymentMethodDetails);
    }

    public PaymentResponseDto refundPayment(String originalTransactionId, String paymentGatewayUsed, Integer amount, String currency) {
        PaymentStrategy strategy = getStrategy(paymentGatewayUsed);
        return strategy.refundPayment(originalTransactionId, amount, currency);
    }

    private String determineStrategyKey(PaymentRequestDto paymentRequest, PassengerPaymentMethod paymentMethodDetails) {
        if (paymentMethodDetails != null) {
            return paymentMethodDetails.getMethodType().name();
        } else if (paymentRequest.getPaymentMethod() != null) {
            return paymentRequest.getPaymentMethod();
        }
        throw new PaymentServiceException("无法确定支付策略");
    }
}