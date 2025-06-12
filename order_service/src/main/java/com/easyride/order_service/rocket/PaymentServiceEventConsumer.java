package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.OrderPaymentSettledEvent;
import com.easyride.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.payment-topic}",
        consumerGroup = "${rocketmq.consumer.payment-group}"
)
public class PaymentServiceEventConsumer implements RocketMQListener<OrderPaymentSettledEvent> {

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(OrderPaymentSettledEvent event) {
        log.info("Received OrderPaymentSettledEvent for orderId: {}", event.getOrderId());
        try {
            orderService.completeOrder(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderPaymentSettledEvent for orderId: {}", event.getOrderId(), e);
            // Handle failure
        }
    }
}
