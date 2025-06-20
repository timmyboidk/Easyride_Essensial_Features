package com.easyride.payment_service.util;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.model.PaymentMethodType;
import org.springframework.stereotype.Component;
import lombok.Builder;
import lombok.Data;

@Component
public class PaymentGatewayUtil {

    /**
     * 模拟支付处理，返回支付成功
     */
    public boolean processPayment(PaymentRequestDto paymentRequestDto) {
        // 实际应调用支付渠道的 SDK
        // 可以根据 paymentRequestDto 的信息进行模拟验证
        return true;  // 模拟支付成功
    }

    /**
     * 模拟退款处理，返回退款成功
     */
    public boolean refundPayment(Long paymentId, Integer amount) {
        // 实际应调用支付渠道退款接口
        return true;  // 模拟退款成功
    }

    public GatewayProcessedPaymentMethod processAndStorePaymentMethodNonce(Long passengerId, String paymentGatewayNonce, PaymentMethodType methodType) {
        // Mock implementation
        return GatewayProcessedPaymentMethod.builder()
                .permanentToken("perm_token_" + System.currentTimeMillis())
                .gatewayCustomerId("cust_" + passengerId)
                .cardLastFour("4242")
                .cardBrand("Visa")
                .expiryMonth(12)
                .expiryYear(2030)
                .build();
    }

    public void deleteGatewayPaymentMethod(String paymentGatewayCustomerId, String paymentGatewayToken, PaymentMethodType methodType) {
        // Mock implementation
    }

    @Data
    @Builder
    public static class GatewayProcessedPaymentMethod {
        private String permanentToken;
        private String gatewayCustomerId;
        private String cardLastFour;
        private String cardBrand;
        private Integer expiryMonth;
        private Integer expiryYear;
    }
}