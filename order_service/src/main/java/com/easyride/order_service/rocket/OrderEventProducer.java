package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.OrderCreatedEvent;
import com.easyride.order_service.dto.OrderEventDto; // Generic event DTO for various order states
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String ORDER_TOPIC = "order-topic";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        // Tag for specific consumption by Matching Service
        rocketMQTemplate.convertAndSend(ORDER_TOPIC + ":ORDER_CREATED", event);
        log.info("Sent ORDER_CREATED event: {}", event);
    }

    public void sendOrderStatusUpdateEvent(OrderEventDto event) {
        // Generic event for status updates, tag can be the status itself or a more general "ORDER_STATUS_UPDATED"
        // Example: order-topic:ORDER_COMPLETED, order-topic:ORDER_CANCELLED
        rocketMQTemplate.convertAndSend(ORDER_TOPIC + ":" + event.getEventType(), event);
        log.info("Sent Order Event ({}): {}", event.getEventType(), event);
    }

    public void sendOrderPaymentSettledEvent(OrderPaymentSettledEvent event) {
        rocketMQTemplate.convertAndSend(ORDER_TOPIC + ":ORDER_PAYMENT_SETTLED", event);
        log.info("Sent ORDER_PAYMENT_SETTLED event: {}", event);
    }
}