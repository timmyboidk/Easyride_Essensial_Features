package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;

import java.util.Map;

public interface PaymentService {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto);
    void handlePaymentNotification(Map<String, String> notificationData);
    void refundPayment(Long paymentId, Integer amount);
    void processOrderPayment(Long orderId); // 新增方法

    @Service
    public class PaymentService {

        @Autowired
        private List<PaymentStrategy> paymentStrategies;

        public PaymentResult processPayment(PaymentMethod method, PaymentRequest request) {
            return paymentStrategies.stream()
                    .filter(strategy -> strategy.supports(method))
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedPaymentMethodException("Method not supported"))
                    .process(request);
        }
    }
}


