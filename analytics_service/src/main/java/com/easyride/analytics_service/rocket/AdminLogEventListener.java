package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AdminLogEvent;
import com.easyride.analytics_service.dto.AnalyticsRequestDto;
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
@RocketMQMessageListener(topic = "EASYRIDE_ADMIN_OPERATION_LOG_TOPIC", consumerGroup = "${rocketmq.consumer.group}")
public class AdminLogEventListener implements RocketMQListener<AdminLogEvent> {

    private static final Logger log = LoggerFactory.getLogger(AdminLogEventListener.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void onMessage(AdminLogEvent event) {
        log.info("Received AdminLogEvent: {}", event);
        try {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("adminId", String.valueOf(event.getAdminId()));
            dimensions.put("operationType", event.getOperationType());
            dimensions.put("targetEntityId", event.getTargetEntityId());
            dimensions.put("ipAddress", event.getIpAddress());

            AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
            analyticsRequest.setRecordType(RecordType.SYSTEM_LOG.name()); // Assuming RecordType has SYSTEM_LOG or
                                                                          // similar
            analyticsRequest.setMetricName("admin_operation");
            analyticsRequest.setMetricValue(1.0);
            analyticsRequest.setRecordTime(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            analyticsRequest.setDimensions(dimensions);

            analyticsService.recordAnalyticsData(analyticsRequest);
        } catch (Exception e) {
            log.error("Error processing AdminLogEvent: ", e);
        }
    }
}
