package com.easyride.payment_service.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMqConfig {
    @Bean
    public RocketMQTemplate rocketMQTemplate() {

        return new RocketMQTemplate();
    }
}
