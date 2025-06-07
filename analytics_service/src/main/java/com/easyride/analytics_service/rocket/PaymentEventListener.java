package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.ConsumedPaymentEventDto;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.service.AnalyticsService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "payment-topic",
        consumerGroup = "${rocketmq.consumer.group}",
        selectorExpression = "PAYMENT_COMPLETED || PAYMENT_FAILED || PAYMENT_REFUNDED"
)
public class PaymentEventListener implements RocketMQListener<ConsumedPaymentEventDto> {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(ConsumedPaymentEventDto event) {
        log.info("Received ConsumedPaymentEvent: {}", event);
        try {
            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());

            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            dimensions.put("passengerId", String.valueOf(event.getPassengerId()));
            // dimensions.put("driverId", String.valueOf(event.getDriverId())); // If available
            dimensions.put("currency", event.getCurrency());
            analyticsRequest.setDimensions(dimensions);

            switch (event.getEventType()) {
                case "PAYMENT_COMPLETED":
                    analyticsRequest.setRecordType(RecordType.PAYMENT_TRANSACTION.name());
                    analyticsRequest.setMetricName("successful_payment_value");
                    analyticsRequest.setMetricValue(event.getAmount());
                    break;
                case "PAYMENT_FAILED":
                    analyticsRequest.setRecordType(RecordType.PAYMENT_FAILED.name());
                    analyticsRequest.setMetricName("failed_payment_count");
                    analyticsRequest.setMetricValue(1.0);
                    dimensions.put("failureReason", event.getFailureReason());
                    break;
                case "PAYMENT_REFUNDED":
                    analyticsRequest.setRecordType(RecordType.PAYMENT_REFUNDED.name());
                    analyticsRequest.setMetricName("refunded_payment_value");
                    analyticsRequest.setMetricValue(event.getAmount()); // Refunded amount
                    // Could also add a count metric for number of refunds
                    break;
                default:
                    log.warn("Unhandled payment event type: {}", event.getEventType());
                    return;
            }
            analyticsService.recordAnalyticsData(analyticsRequest);
        } catch (Exception e) {
            log.error("Error processing payment event for analytics: ", e);
        }
    }
}