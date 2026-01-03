package com.easyride.payment_service.rocketmq;

import com.easyride.payment_service.dto.PaymentEventDto;
import com.easyride.payment_service.dto.PaymentFailedEventDto;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String PAYMENT_TOPIC = "EASYRIDE_PAYMENT_SUCCESS_TOPIC";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendPaymentEvent(PaymentEventDto event) {
        // Use eventType for the tag, as it's more descriptive
        String tag = event.getEventType() != null ? event.getEventType() : "UNKNOWN_EVENT";
        rocketMQTemplate.convertAndSend(PAYMENT_TOPIC + ":" + tag, event);
        log.info("Sent PaymentEvent (Tag: {}): {}", tag, event);
    }

    public void sendPaymentFailedEvent(PaymentFailedEventDto event) {
        rocketMQTemplate.convertAndSend(PAYMENT_TOPIC + ":PAYMENT_FAILED", event);
        log.info("Sent PaymentFailedEvent: {}", event);
    }

    public void sendPaymentEventOrderly(PaymentEventDto event, String shardingKey) {
        String tag = event.getEventType() != null ? event.getEventType() : "ORDERLY_UPDATE";
        rocketMQTemplate.syncSendOrderly(PAYMENT_TOPIC + ":" + tag,
                MessageBuilder.withPayload(event).build(),
                shardingKey);
        log.info("Sent Orderly PaymentEvent (Tag: {}, ShardingKey: {}): {}", tag, shardingKey, event);
    }
}