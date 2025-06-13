package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.PaymentConfirmedEventDto;
import com.easyride.order_service.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.payment-topic}",
        consumerGroup = "${rocketmq.consumer.payment-group}"
)
public class PaymentServiceEventConsumer implements RocketMQListener<PaymentConfirmedEventDto> {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceEventConsumer.class);

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(PaymentConfirmedEventDto event) {
        log.info("Received PaymentConfirmedEvent: {}", event);
        try {
            orderService.processPaymentConfirmation(event.getOrderId(), event.getFinalAmount(), event.getPaymentTransactionId());
        } catch (Exception e) {
            log.error("Error processing PaymentConfirmedEvent for orderId {}: ", event.getOrderId(), e);
            // Handle error, possibly by retrying or sending to a dead-letter-queue
        }
    }
}