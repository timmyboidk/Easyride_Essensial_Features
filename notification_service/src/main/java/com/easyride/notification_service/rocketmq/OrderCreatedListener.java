package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.OrderCreatedEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_CREATED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class OrderCreatedListener implements RocketMQListener<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final NotificationDispatcherService dispatcherService;

    public OrderCreatedListener(NotificationDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: OrderID={}", event.getOrderId());
        try {
            dispatcherService.dispatchOrderCreated(event);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent: ", e);
        }
    }
}
