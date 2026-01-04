package com.easyride.payment_service.rocketmq;

import com.easyride.payment_service.dto.OrderDetailsForPaymentDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.service.PaymentService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_STATUS_CHANGED_TOPIC", consumerGroup = "CID_PAYMENT_SERVICE", selectorExpression = "COMPLETED")
public class OrderEventConsumer implements RocketMQListener<OrderDetailsForPaymentDto> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void onMessage(OrderDetailsForPaymentDto orderDetailsEvent) {
        log.info("Received OrderDetailsForPaymentEvent: {}", orderDetailsEvent);
        try {
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setOrderId(orderDetailsEvent.getOrderId());
            paymentRequest.setPassengerId(orderDetailsEvent.getPassengerId());
            // Convert Double to Integer (cents)
            paymentRequest.setAmount((int) (orderDetailsEvent.getFinalAmount() * 100));
            paymentRequest.setCurrency(orderDetailsEvent.getCurrency());
            paymentRequest.setPaymentMethod(orderDetailsEvent.getPaymentMethodTypeString());
            paymentRequest.setPaymentMethodId(orderDetailsEvent.getChosenPaymentMethodId());

            paymentService.associateDriverWithOrderPayment(orderDetailsEvent.getOrderId(),
                    orderDetailsEvent.getDriverId());
            paymentService.processPayment(paymentRequest);

        } catch (Exception e) {
            log.error("Error processing OrderDetailsForPaymentEvent for orderId {}: ", orderDetailsEvent.getOrderId(),
                    e);
        }
    }
}