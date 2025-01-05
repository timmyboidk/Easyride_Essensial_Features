package com.easyride.admin_service.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 如果要在 application.yml 中写明 rocketmq:
 *   name-server: 127.0.0.1:9876
 *   producer:
 *     group: admin-producer-group
 *   consumer:
 *     group: admin-consumer-group
 * 即可自动配置 RocketMQ。
 */
@Configuration
public class RocketMqConfig {

    /**
     * 如果不需要额外定制，RocketMQTemplate 也可通过自动装配生成。
     * 这里示例一个自定义 Bean：
     */
    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}
