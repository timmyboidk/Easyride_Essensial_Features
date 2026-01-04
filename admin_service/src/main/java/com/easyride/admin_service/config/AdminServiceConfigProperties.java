package com.easyride.admin_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class AdminServiceConfigProperties {

    @Data
    @Component
    @ConfigurationProperties(prefix = "service-urls")
    public static class ServiceUrls {
        private String userService;
        private String orderService;
        private String paymentService;
        private String reviewService;
        private String locationService;
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "easyride")
    public static class EasyRide {
        private Admin admin;
        private SystemConfig system;

        @Data
        public static class Admin {
            private Integer defaultPageSize;
        }

        @Data
        public static class SystemConfig {
            private Pricing pricing;
        }

        @Data
        public static class Pricing {
            private Double baseFare;
            private Double perKmRate;
        }
    }

    @Data
    @Component
    @ConfigurationProperties(prefix = "rocketmq.topic")
    public static class RocketMqTopic {
        private String driverReview;
    }
}
