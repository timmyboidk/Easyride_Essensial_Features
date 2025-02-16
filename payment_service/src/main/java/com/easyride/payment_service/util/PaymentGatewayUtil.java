package com.easyride.payment_service.util;

import com.easyride.payment_service.dto.PaymentRequestDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayUtil {

    /**
     * 模拟支付处理，返回支付成功
     */
    public boolean processPayment(PaymentRequestDto paymentRequestDto) {
        // 实际应调用支付渠道的 SDK，这里简单模拟
        // 可以根据 paymentRequestDto 的信息进行模拟验证
        return true;  // 模拟支付成功
    }

    /**
     * 模拟退款处理，返回退款成功
     */
    public boolean refundPayment(Long paymentId, Integer amount) {
        // 实际应调用支付渠道退款接口，此处简单模拟
        return true;  // 模拟退款成功
    }
}
