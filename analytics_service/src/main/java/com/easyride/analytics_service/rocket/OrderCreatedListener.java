package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.OrderCreatedEvent;
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
@RocketMQMessageListener(topic = "EASYRIDE_ORDER_CREATED_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class OrderCreatedListener implements RocketMQListener<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event);
        try {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("orderId", String.valueOf(event.getOrderId()));
            dimensions.put("passengerId", String.valueOf(event.getPassengerId()));
            dimensions.put("serviceType", event.getServiceType());

            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordType(RecordType.ORDER_REQUEST.name());
            analyticsRequest.setMetricName("order_created_count");
            analyticsRequest.setMetricValue(1.0);
            analyticsRequest
                    .setRecordTime(event.getCreatedTime() != null ? event.getCreatedTime() : LocalDateTime.now());
            analyticsRequest.setDimensions(dimensions);

            analyticsService.recordAnalyticsData(analyticsRequest);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent: ", e);
        }
    }
}
