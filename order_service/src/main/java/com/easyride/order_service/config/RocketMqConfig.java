package com.easyride.order_service.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMqConfig {

//    // Producer configuration
//    @Bean
//    public ProducerFactory<String, UserEventDto> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(
//            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
//            "localhost:9092"
//        );
//        configProps.put(
//            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
//            StringSerializer.class
//        );
//        configProps.put(
//            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
//            org.springframework.kafka.support.serializer.JsonSerializer.class
//        );
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }
//
//    @Bean
//    public KafkaTemplate<String, UserEventDto> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//    @Bean
//public ConsumerFactory<String, UserEventDto> consumerFactory() {
//    Map<String, Object> props = new HashMap<>();
//    props.put(
//        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
//        "localhost:9092"
//    );
//    props.put(
//        ConsumerConfig.GROUP_ID_CONFIG,
//        "order-service-group"
//    );
//    props.put(
//        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//        StringDeserializer.class
//    );
//    props.put(
//        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//        JsonDeserializer.class
//    );
//    props.put(
//        JsonDeserializer.TRUSTED_PACKAGES,
//        "com.easyride.order_service.dto"
//    );
//    return new DefaultKafkaConsumerFactory<>(props);
//}
//
//@Bean
//public ConcurrentKafkaListenerContainerFactory<String, UserEventDto> kafkaListenerContainerFactory() {
//    ConcurrentKafkaListenerContainerFactory<String, UserEventDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
//    factory.setConsumerFactory(consumerFactory());
//    return factory;
//}
        @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
}
