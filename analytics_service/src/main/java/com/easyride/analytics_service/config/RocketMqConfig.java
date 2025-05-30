package com.easyride.analytics_service.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 */
@Configuration
public class RocketMqConfig {

    /**
     * 如果需要对 RocketMQTemplate 做进一步定制，可在此注入并修改属性。
     */
    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}
