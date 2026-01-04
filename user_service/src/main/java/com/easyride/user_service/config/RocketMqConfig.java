package com.easyride.user_service.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMqConfig {

    // Producer configuration
    // @Bean
    // public ProducerFactory<String, UserEventDto> producerFactory() {
    // Map<String, Object> configProps = new HashMap<>();
    // configProps.put(
    // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
    // "localhost:9092"
    // );
    // configProps.put(
    // ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    // StringSerializer.class
    // );
    // configProps.put(
    // ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
    // org.springframework.kafka.support.serializer.JsonSerializer.class
    // );
    // return new DefaultKafkaProducerFactory<>(configProps);
    // }

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}
