package com.easyride.payment_service.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private Long orderId;

    private Long passengerId;

    // 修改为 Integer 类型，单位为最小货币单位，例如 100 分表示 1 元
    // 修改为 Integer 类型，单位为最小货币单位，例如 100 分表示 1 元
    @jakarta.validation.constraints.NotNull(message = "金额不能为空")
    @Min(value = 1, message = "金额必须大于0")
    private Integer amount;

    private String paymentMethod; // PAYPAL, CREDIT_CARD, BALANCE

    private String currency; // USD, CNY, etc.

    // 其他支付渠道需要的参数
    private Long paymentMethodId;
    private String paymentGatewayNonce;
}
