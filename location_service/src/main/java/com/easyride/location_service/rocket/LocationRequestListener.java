package com.easyride.location_service.rocket;

import com.easyride.location_service.dto.LocationRequestEvent; // Import the new DTO
import com.easyride.location_service.dto.LocationResponseEvent;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "location-request-topic", consumerGroup = "CID_LOCATION_SERVICE")
public class LocationRequestListener implements RocketMQListener<LocationRequestEvent> {

    private final RocketMQTemplate rocketMQTemplate;
    private final LocationService locationService;

    public LocationRequestListener(RocketMQTemplate rocketMQTemplate, LocationService locationService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.locationService = locationService;
    }

    @Override
    public void onMessage(LocationRequestEvent event) {
        LocationResponse response = locationService.getLocationInfo(event.getLatitude(), event.getLongitude());
        String formattedAddress = "";
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            // Corrected method name from getFormattedAddress to getFormatted_address
            formattedAddress = response.getResults().get(0).getFormatted_address();
        }

        // The LocationResponseEvent class is also missing and would need to be created.
        // Assuming it has a constructor (correlationId, address, placeId)
        LocationResponseEvent respEvent = new LocationResponseEvent(
                event.getCorrelationId(),
                formattedAddress,
                "PlaceIdXYZ" // This could also be extracted from the response if available
        );

        rocketMQTemplate.convertAndSend("location-response-topic", respEvent);
    }
}