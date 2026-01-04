package com.easyride.payment_service.util;

import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentGatewayUtilTest {

    private final PaymentGatewayUtil paymentGatewayUtil = new PaymentGatewayUtil();

    @Test
    void processPayment_Success() {
        boolean result = paymentGatewayUtil.processPayment(new PaymentRequestDto());
        assertTrue(result);
    }

    @Test
    void refundPayment_Success() {
        boolean result = paymentGatewayUtil.refundPayment(1L, 100);
        assertTrue(result);
    }

    @Test
    void processAndStorePaymentMethodNonce_Success() {
        PaymentGatewayUtil.GatewayProcessedPaymentMethod method = paymentGatewayUtil
                .processAndStorePaymentMethodNonce(1L, "nonce", PaymentMethodType.CREDIT_CARD);
        assertNotNull(method);
        assertNotNull(method.getPermanentToken());
        assertNotNull(method.getGatewayCustomerId());
    }
}
