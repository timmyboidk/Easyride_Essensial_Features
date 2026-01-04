package com.evaluation.rocketmq;

import com.evaluation.dto.PaymentSuccessEvent;
import com.evaluation.service.ReviewWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "EASYRIDE_PAYMENT_SUCCESS_TOPIC", consumerGroup = "CID_REVIEW_SERVICE")
public class PaymentSuccessConsumer implements RocketMQListener<PaymentSuccessEvent> {

    private final ReviewWindowService reviewWindowService;

    @Override
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received payment success event for order: {}", event.getOrderId());
        reviewWindowService.openReviewWindow(event.getOrderId(), null, null, event.getPaymentTime());
    }
}
