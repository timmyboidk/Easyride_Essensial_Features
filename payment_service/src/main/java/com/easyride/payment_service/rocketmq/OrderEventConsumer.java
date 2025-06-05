package com.easyride.payment_service.rocketmq;

// import com.easyride.payment_service.dto.OrderEventDto; // This might be too generic
import com.easyride.payment_service.dto.OrderDetailsForPaymentDto;
import com.easyride.payment_service.dto.PaymentRequestDto;
import com.easyride.payment_service.service.PaymentService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "order-topic", // Listen to events from Order Service
        consumerGroup = "payment-service-order-consumer-group",
        // Tag might be ORDER_COMPLETED_FOR_PAYMENT or similar from Order Service
        selectorExpression = "ORDER_COMPLETED_FOR_PAYMENT"
)
public class OrderEventConsumer implements RocketMQListener<OrderDetailsForPaymentDto> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final PaymentService paymentService;

    @Autowired
    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void onMessage(OrderDetailsForPaymentDto orderDetailsEvent) {
        log.info("Received OrderDetailsForPaymentEvent: {}", orderDetailsEvent);
        try {
            // Construct PaymentRequestDto from the event
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setOrderId(orderDetailsEvent.getOrderId());
            paymentRequest.setPassengerId(orderDetailsEvent.getPassengerId());
            paymentRequest.setAmount(orderDetailsEvent.getFinalAmount());
            paymentRequest.setCurrency(orderDetailsEvent.getCurrency());
            paymentRequest.setPaymentMethod(orderDetailsEvent.getPaymentMethodTypeString()); // Map this to strategy key
            paymentRequest.setPaymentMethodId(orderDetailsEvent.getChosenPaymentMethodId()); // ID of stored payment method
            // PaymentService will use passengerId and paymentMethodId to fetch full PassengerPaymentMethod

            // Store driverId with the payment record if not already done,
            // or pass it to wallet service directly if payment succeeds immediately.
            // One way is to update the Payment entity with driverId here or in processPayment.
            // For now, processPayment will attempt to get it.

            // We can also update our local Payment record with driverId if it exists,
            // before processing, so WalletService can use it.
            paymentService.associateDriverWithOrderPayment(orderDetailsEvent.getOrderId(), orderDetailsEvent.getDriverId());


            paymentService.processPayment(paymentRequest);

        } catch (Exception e) {
            log.error("Error processing OrderDetailsForPaymentEvent for orderId {}: ", orderDetailsEvent.getOrderId(), e);
            // Handle error, DLQ, etc.
        }
    }
}