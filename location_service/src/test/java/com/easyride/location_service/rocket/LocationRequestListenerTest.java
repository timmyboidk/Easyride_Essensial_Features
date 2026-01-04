package com.easyride.location_service.rocket;

import com.easyride.location_service.dto.LocationRequestEvent;
import com.easyride.location_service.dto.LocationResponseEvent;
import com.easyride.location_service.model.LocationResponse;
import com.easyride.location_service.service.LocationService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationRequestListenerTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationRequestListener listener;

    @Test
    void onMessage_Success() {
        LocationRequestEvent event = new LocationRequestEvent();
        event.setCorrelationId("123");
        event.setLatitude(10.0);
        event.setLongitude(20.0);

        LocationResponse response = new LocationResponse();
        LocationResponse.Result result = new LocationResponse.Result();
        result.setFormatted_address("Test Address");
        response.setResults(Collections.singletonList(result));

        when(locationService.getLocationInfo(10.0, 20.0)).thenReturn(response);

        listener.onMessage(event);

        verify(rocketMQTemplate).convertAndSend(eq("location-response-topic"), any(LocationResponseEvent.class));
    }
}
