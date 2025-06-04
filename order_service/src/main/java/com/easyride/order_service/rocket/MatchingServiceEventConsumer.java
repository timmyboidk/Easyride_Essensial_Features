package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.DriverAssignedEventDto;
import com.easyride.order_service.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "order-topic", // Or a dedicated topic from Matching Service like "matching-result-topic"
        consumerGroup = "order-service-matching-consumer-group",
        selectorExpression = "DRIVER_ASSIGNED || ORDER_MATCH_FAILED" // Listen to successful assignments or failures
)
public class MatchingServiceEventConsumer implements RocketMQListener<Object> { // Use Object or a common base event type

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceEventConsumer.class);

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(Object message) {
        if (message instanceof DriverAssignedEventDto event) {
            log.info("Received DRIVER_ASSIGNED event: {}", event);
            try {
                orderService.processDriverAssigned(event);
            } catch (Exception e) {
                log.error("Error processing DRIVER_ASSIGNED event for orderId {}: ", event.getOrderId(), e);
                // Handle error, possibly retry or DLQ
            }
        } else if (message instanceof OrderMatchFailedEventDto event) { // Define OrderMatchFailedEventDto
            log.warn("Received ORDER_MATCH_FAILED event for orderId {}: {}", event.getOrderId(), event.getReason());
            orderService.processOrderMatchFailed(event.getOrderId(), event.getReason());
        } else {
            log.warn("Received unknown event type from matching service topic: {}", message);
        }
    }
}