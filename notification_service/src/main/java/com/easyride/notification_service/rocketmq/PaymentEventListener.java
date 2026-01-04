package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.PaymentSuccessEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_PAYMENT_SUCCESS_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class PaymentEventListener implements RocketMQListener<PaymentSuccessEvent> {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Override
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: PaymentID={}", event.getPaymentId());
        try {
            dispatcherService.dispatchPaymentSuccess(event);
        } catch (Exception e) {
            log.error("Error processing PaymentSuccessEvent: ", e);
        }
    }
}
