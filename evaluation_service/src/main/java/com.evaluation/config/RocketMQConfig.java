package com.evaluation.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;

@Configuration
@EnableAutoConfiguration(exclude = RocketMQAutoConfiguration.class)
public class RocketMQConfig {
    // 配置内容
}
