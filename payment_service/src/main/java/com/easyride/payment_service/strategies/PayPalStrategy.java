package com.easyride.payment_service.strategies;

import com.easyride.payment_service.service.PaymentStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "payment.method", havingValue = "paypal")
public class PayPalStrategy implements PaymentStrategy {

    @Override
    public PaymentResult process(PaymentRequest request) {
        // PayPal SDK 集成
    }
}
