package com.easyride.payment_service.model;

public enum PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    APPLE_PAY, // Might be tokenized representation
    GOOGLE_PAY, // Might be tokenized representation
    ALIPAY,
    WECHAT_PAY
}