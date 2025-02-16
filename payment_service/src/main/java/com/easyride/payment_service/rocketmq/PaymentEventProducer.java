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

    /**
     * 发送普通消息
     */
    public void sendPaymentEvent(PaymentEventDto paymentEvent) {
        rocketMQTemplate.convertAndSend("payment-topic", paymentEvent);
    }

    /**
     * 发送带 tag 的有序消息
     * @param tag 标签，例如 "PAYMENT_COMPLETED" 或 "REFUND"
     * @param paymentEvent 支付事件对象
     * @param partitionKey 分区键，通常使用订单ID的字符串形式
     */
    public SendResult sendPaymentEventOrderly(String tag, PaymentEventDto paymentEvent, String partitionKey) {
        String topicWithTag = "payment-topic:" + tag;
        return rocketMQTemplate.syncSendOrderly(topicWithTag, paymentEvent, partitionKey);
    }
}
