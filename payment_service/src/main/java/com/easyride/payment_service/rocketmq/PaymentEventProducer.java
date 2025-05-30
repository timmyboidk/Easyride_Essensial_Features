package com.easyride.payment_service.rocketmq;

import com.easyride.payment_service.dto.PaymentEventDto;
import org.apache.rocketmq.client.producer.SendResult;
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

    public SendResult sendPaymentEventOrderly(String tag, PaymentEventDto paymentEvent, String partitionKey) {
        String topicWithTag = "payment-topic:" + tag;
        return rocketMQTemplate.syncSendOrderly(topicWithTag, paymentEvent, partitionKey);
    }
}
