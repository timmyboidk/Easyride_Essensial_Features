package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.OrderStatusChangedEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_STATUS_CHANGED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class OrderStatusChangedListener implements RocketMQListener<OrderStatusChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedListener.class);

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Override
    public void onMessage(OrderStatusChangedEvent event) {
        log.info("Received OrderStatusChangedEvent: OrderID={}, Status={}", event.getOrderId(), event.getNewStatus());
        try {
            dispatcherService.dispatchOrderStatusChanged(event);
        } catch (Exception e) {
            log.error("Error processing OrderStatusChangedEvent: ", e);
        }
    }
}
