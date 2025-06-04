package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.PaymentConfirmedEventDto; // Assuming Payment Service sends this
import com.easyride.order_service.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "payment-topic", // Listen to events from Payment Service
        consumerGroup = "order-service-payment-consumer-group",
        selectorExpression = "PAYMENT_COMPLETED || PAYMENT_SETTLED" // Or whatever Payment Service sends
)
public class PaymentServiceEventConsumer implements RocketMQListener<PaymentConfirmedEventDto> {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceEventConsumer.class);

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(PaymentConfirmedEventDto event) {
        log.info("Received PaymentConfirmedEvent: {}", event);
        try {
            // Update order status to PAYMENT_SETTLED and publish the ORDER_PAYMENT_SETTLED event
            orderService.processPaymentConfirmation(event.getOrderId(), event.getFinalAmount(), event.getPaymentTransactionId());
        } catch (Exception e) {
            log.error("Error processing PaymentConfirmedEvent for orderId {}: ", event.getOrderId(), e);
            // Handle error
        }
    }
}