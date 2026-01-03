package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.OrderCreatedEvent;
import com.easyride.order_service.dto.OrderEventDto;
import com.easyride.order_service.dto.OrderPaymentSettledEvent;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.topics.order-created:order-topic}")
    private String orderCreatedTopic;

    @Value("${rocketmq.producer.topics.order-status:order-status-topic}")
    private String orderStatusTopic;

    @Value("${rocketmq.producer.topics.payment-settled:payment-settled-topic}")
    private String paymentSettledTopic;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending OrderCreatedEvent for orderId: {}", event.getOrderId());
        // Using the injected RocketMQTemplate to send the message
        rocketMQTemplate.convertAndSend(orderCreatedTopic, event);
    }

    public void sendOrderStatusUpdateEvent(OrderEventDto event) {
        log.info("Publishing Order Status Update event for orderId: {}. Status: {}", event.getOrderId(),
                event.getStatus());
        rocketMQTemplate.convertAndSend(orderStatusTopic, event);
    }

    // Added the missing method to send payment settled events
    public void sendOrderPaymentSettledEvent(OrderPaymentSettledEvent event) {
        log.info("Publishing ORDER_PAYMENT_SETTLED event for orderId: {}", event.getOrderId());
        rocketMQTemplate.convertAndSend(paymentSettledTopic, event);
    }

}