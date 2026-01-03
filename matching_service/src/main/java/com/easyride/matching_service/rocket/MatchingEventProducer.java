package com.easyride.matching_service.rocket;

import com.easyride.matching_service.dto.DriverAssignedEventDto;
import com.easyride.matching_service.dto.OrderMatchFailedEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchingEventProducer {
    private static final Logger log = LoggerFactory.getLogger(MatchingEventProducer.class);
    // Topic should be consumed by Order Service primarily.
    // Using "order-topic" as per previous examples, with specific tags.
    private static final String MATCHING_RESULT_TOPIC = "EASYRIDE_MATCHING_RESULT_TOPIC";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendDriverAssignedEvent(DriverAssignedEventDto event) {
        rocketMQTemplate.convertAndSend(MATCHING_RESULT_TOPIC + ":DRIVER_ASSIGNED", event);
        log.info("Sent DRIVER_ASSIGNED event for order {}: {}", event.getOrderId(), event);
    }

    public void sendOrderMatchFailedEvent(OrderMatchFailedEventDto event) {
        rocketMQTemplate.convertAndSend(MATCHING_RESULT_TOPIC + ":ORDER_MATCH_FAILED", event);
        log.info("Sent ORDER_MATCH_FAILED event for order {}: {}", event.getOrderId(), event);
    }
}