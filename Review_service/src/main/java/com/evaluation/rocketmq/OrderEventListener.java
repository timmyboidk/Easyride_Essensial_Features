package com.evaluation.rocketmq;

import com.evaluation.dto.OrderCompletedReviewEventDto;
import com.evaluation.service.ReviewWindowService; // New service to manage review windows
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "order-topic", // Or "payment-topic" if ORDER_PAYMENT_SETTLED is the trigger
        consumerGroup = "review-service-order-consumer-group",
        selectorExpression = "ORDER_PAYMENT_SETTLED" // Or ORDER_COMPLETED, whichever signifies review window opens
)
public class OrderEventListener implements RocketMQListener<OrderCompletedReviewEventDto> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Autowired
    private ReviewWindowService reviewWindowService; // You'll need to create this service

    @Override
    public void onMessage(OrderCompletedReviewEventDto event) {
        log.info("Received OrderCompletedReviewEvent for order ID: {}, enabling review window.", event.getOrderId());
        try {
            reviewWindowService.openReviewWindow(
                    event.getOrderId(),
                    event.getPassengerId(),
                    event.getDriverId(),
                    event.getTripEndTime()
            );
        } catch (Exception e) {
            log.error("Error opening review window for order {}: ", event.getOrderId(), e);
            // Handle error
        }
    }
}