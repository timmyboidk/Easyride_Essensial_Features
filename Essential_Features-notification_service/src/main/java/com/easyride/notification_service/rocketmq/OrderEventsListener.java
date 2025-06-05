package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.ConsumedOrderEventDto;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "order-topic", // Topic from Order Service
        consumerGroup = "${rocketmq.consumer.group}", // Use property for group
        selectorExpression = "ORDER_ACCEPTED || DRIVER_ASSIGNED || DRIVER_ARRIVED || ORDER_COMPLETED || ORDER_CANCELLED_PASSENGER || ORDER_SCHEDULED_REMINDER" // Consume relevant tags
)
public class OrderEventsListener implements RocketMQListener<ConsumedOrderEventDto> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Override
    public void onMessage(ConsumedOrderEventDto event) {
        log.info("Received ConsumedOrderEvent: Type={}, OrderID={}", event.getEventType(), event.getOrderId());
        try {
            // Here, event should ideally contain user contact info (phone, email, push tokens)
            // and locale preferences, fetched by the publishing service (OrderService)
            // or NotificationService needs to fetch them from User Service if not included.
            // For simplicity, assuming ConsumedOrderEventDto is enriched.
            dispatcherService.dispatchOrderNotification(event);
        } catch (Exception e) {
            log.error("Error processing order event for notification: ", e);
            // DLQ or error handling
        }
    }
}