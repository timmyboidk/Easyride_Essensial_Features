package com.easyride.payment_service.rocketmq;

import com.easyride.payment_service.dto.PaymentEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public PaymentEventProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void sendPaymentEvent(PaymentEventDto paymentEvent) {
        rocketMQTemplate.convertAndSend("payment-topic", paymentEvent);
    }
}
