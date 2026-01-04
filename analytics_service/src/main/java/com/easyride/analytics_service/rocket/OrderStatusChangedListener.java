package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.OrderStatusChangedEventDto;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.service.AnalyticsService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_STATUS_CHANGED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class OrderStatusChangedListener implements RocketMQListener<OrderStatusChangedEventDto> {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(OrderStatusChangedEventDto event) {
        log.info("Received OrderStatusChangedEvent: {}", event);
        try {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            if (event.getDriverId() != null) {
                dimensions.put("driverId", String.valueOf(event.getDriverId()));
            }

            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            analyticsRequest.setDimensions(dimensions);

            String status = event.getNewStatus(); // Assuming DTO has this field

            if ("COMPLETED".equalsIgnoreCase(status)) {
                // Assuming we might get fare info in status changed or we rely on payment
                // settled.
                // But MQ.md says "Order Funnel".
                analyticsRequest.setRecordType(RecordType.COMPLETED_ORDERS_COUNT.name());
                analyticsRequest.setMetricName("completed_order_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);
            } else if ("CANCELLED".equalsIgnoreCase(status)) {
                analyticsRequest.setRecordType(RecordType.CANCELLED_ORDERS_COUNT.name());
                analyticsRequest.setMetricName("cancelled_order_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);
            } else if ("MATCHED".equalsIgnoreCase(status) || "DRIVER_ASSIGNED".equalsIgnoreCase(status)) {
                analyticsRequest.setRecordType(RecordType.ORDER_ACCEPTED_BY_DRIVER.name());
                analyticsRequest.setMetricName("order_matched_count");
                analyticsRequest.setMetricValue(1.0);
                analyticsService.recordAnalyticsData(analyticsRequest);
            }

        } catch (Exception e) {
            log.error("Error processing OrderStatusChangedEvent: ", e);
        }
    }
}
