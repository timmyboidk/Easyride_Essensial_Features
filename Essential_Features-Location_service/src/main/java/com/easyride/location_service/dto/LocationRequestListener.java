package com.easyride.locationService.dto;

import com.easyride.locationService.dto.LocationResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "location-request-topic", consumerGroup = "location-service-group")
public class LocationRequestListener implements RocketMQListener<LocationRequestEvent> {

    private final RocketMQTemplate rocketMQTemplate;
    private final LocationService locationService; // 您给出的 Google Maps 调用

    public LocationRequestListener(RocketMQTemplate rocketMQTemplate, LocationService locationService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.locationService = locationService;
    }

    @Override
    public void onMessage(LocationRequestEvent event) {
        // 1. 调用 locationService.getLocationInfo(event.getLatitude(), event.getLongitude());
        LocationResponse response = locationService.getLocationInfo(event.getLatitude(), event.getLongitude());
        String formattedAddress = ""; 
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            formattedAddress = response.getResults().get(0).getFormattedAddress();
        }
        // 2. 构造 LocationResponseEvent
        LocationResponseEvent respEvent = new LocationResponseEvent(
            event.getCorrelationId(),
            formattedAddress,
            "PlaceIdXYZ" // 也可从 response 中获取
        );
        // 3. 发送回 matching_service 监听的 topic
        rocketMQTemplate.convertAndSend("location-response-topic", respEvent);
    }
}
