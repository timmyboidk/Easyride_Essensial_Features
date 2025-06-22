package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.ConsumedUserEventDto;
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
        topic = "user-topic",
        consumerGroup = "${rocketmq.consumer.group}", // Use property
        selectorExpression = "USER_CREATED || DRIVER_APPLICATION_APPROVED" // Or more specific tags
)
public class UserEventListener implements RocketMQListener<ConsumedUserEventDto> {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(ConsumedUserEventDto event) {
        log.info("Received ConsumedUserEvent: {}", event);
        try {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("userId", String.valueOf(event.getUserId()));
            dimensions.put("userRole", event.getRole());

            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            analyticsRequest.setDimensions(dimensions);

            if ("USER_CREATED".equals(event.getEventType())) {
                if ("DRIVER".equalsIgnoreCase(event.getRole())) {
                    analyticsRequest.setRecordType(RecordType.DRIVER_REGISTRATION.name());
                } else {
                    analyticsRequest.setRecordType(RecordType.USER_REGISTRATION.name());
                }
                analyticsRequest.setMetricName("registration_count");
                analyticsRequest.setMetricValue(1.0);
            } else if ("DRIVER_APPLICATION_APPROVED".equals(event.getEventType())) {
                analyticsRequest.setRecordType(RecordType.DRIVER_APPROVED.name());
                analyticsRequest.setMetricName("approved_driver_count");
                analyticsRequest.setMetricValue(1.0);
            } else {
                log.warn("Unhandled user event type: {}", event.getEventType());
                return;
            }
            analyticsService.recordAnalyticsData(analyticsRequest);
        } catch (Exception e) {
            log.error("Error processing user event for analytics: ", e);
        }
    }
}