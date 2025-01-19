package com.easyride.payment_service.rocketmq;

import com.easyride.payment_service.dto.OrderEventDto;
import com.easyride.payment_service.service.PaymentService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "payment-service-group"
)
public class OrderEventConsumer implements RocketMQListener<OrderEventDto> {

    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void onMessage(OrderEventDto orderEvent) {
        // 根据订单事件执行相应的支付逻辑
        if ("ORDER_COMPLETED".equals(orderEvent.getEventType())) {
            paymentService.processOrderPayment(orderEvent.getOrderId());
        }
        // 处理其他类型的订单事件
    }
}
