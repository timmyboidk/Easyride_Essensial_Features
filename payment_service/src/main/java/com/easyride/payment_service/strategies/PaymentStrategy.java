package com.easyride.payment_service.service;


public interface PaymentStrategy {
    PaymentResult process(PaymentRequest request);
}

@Service
@ConditionalOnProperty(name = "payment.method", havingValue = "paypal")
public class PayPalStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        // PayPal SDK 集成
    }
}