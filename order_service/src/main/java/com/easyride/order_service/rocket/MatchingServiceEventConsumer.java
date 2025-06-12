package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.DriverAssignedEventDto;
import com.easyride.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.matching-topic}",
        consumerGroup = "${rocketmq.consumer.matching-group}"
)
public class MatchingServiceEventConsumer implements RocketMQListener<DriverAssignedEventDto> {

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(DriverAssignedEventDto driverAssignedEvent) {
        log.info("Received DriverAssignedEvent: {}", driverAssignedEvent);
        try {
            orderService.assignDriverToOrder(driverAssignedEvent.getOrderId(), driverAssignedEvent.getDriverId());
        } catch (Exception e) {
            log.error("Failed to process DriverAssignedEvent for orderId: {}", driverAssignedEvent.getOrderId(), e);
            // In a real application, you would handle this failure, e.g., by sending it to a dead-letter queue.
        }
    }
}