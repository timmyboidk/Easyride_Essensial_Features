package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.DriverAssignedEventDto;
import com.easyride.order_service.dto.OrderMatchFailedEventDto;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.matching-topic}",
        consumerGroup = "${rocketmq.consumer.matching-group}"
)
public class MatchingServiceEventConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceEventConsumer.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        try {
            // A simple way to distinguish events is by checking for key fields
            if (message.contains("\"driverId\"")) {
                DriverAssignedEventDto event = objectMapper.readValue(message, DriverAssignedEventDto.class);
                log.info("Received DriverAssignedEvent: {}", event);
                orderService.processDriverAssigned(event);
            } else if (message.contains("\"reason\"")) {
                OrderMatchFailedEventDto event = objectMapper.readValue(message, OrderMatchFailedEventDto.class);
                log.warn("Received OrderMatchFailedEvent: {}", event);
                orderService.processOrderMatchFailed(event.getOrderId(), event.getReason());
            } else {
                log.warn("Received unknown message from matching topic: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process message from matching-topic. Message: {}", message, e);
            // Consider moving to a dead-letter queue for manual inspection
        }
    }
}