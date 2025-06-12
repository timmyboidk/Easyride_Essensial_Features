package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private final String topic = "order-topic";

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending OrderCreatedEvent for orderId: {}", event.getOrderId());
        // Using the injected RocketMQTemplate to send the message
        rocketMQTemplate.convertAndSend(topic, event);
    }
}