package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.dto.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto);

    void handlePaymentNotification(String notificationPayload);

    PaymentResponseDto refundPayment(String internalPaymentId, Integer amountToRefund);

    void processOrderPayment(Long orderId);

    void associateDriverWithOrderPayment(Long orderId, Long driverId);
}