package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;
import com.easyride.payment_service.strategies.PaymentStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto);
    void handlePaymentNotification(Map<String, String> notificationData);
    void refundPayment(Long paymentId, Integer amount);

    PaymentResponseDto refundPayment(String internalPaymentId /*or orderId*/, Double amountToRefund);

    void processOrderPayment(Long orderId); // 新增方法

    // ... (inside PaymentService interface)
    void associateDriverWithOrderPayment(Long orderId, Long driverId);

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


