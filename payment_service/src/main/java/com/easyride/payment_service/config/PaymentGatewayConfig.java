package com.easyride.payment_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment-gateway")
@Data
public class PaymentGatewayConfig {
    private Paypal paypal = new Paypal();
    private Stripe stripe = new Stripe();

    @Data
    public static class Paypal {
        private String clientId;
        private String clientSecret;
        private String mode;
    }

    @Data
    public static class Stripe {
        private String apiKey;
    }
}
