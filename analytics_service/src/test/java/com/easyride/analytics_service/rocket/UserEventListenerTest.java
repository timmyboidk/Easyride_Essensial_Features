package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.ConsumedUserEventDto;
import com.easyride.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private UserEventListener listener;

    @Test
    void onMessage_DriverRegistration() {
        ConsumedUserEventDto event = new ConsumedUserEventDto();
        event.setUserId(1L);
        event.setRole("DRIVER");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }

    @Test
    void onMessage_UserRegistration() {
        ConsumedUserEventDto event = new ConsumedUserEventDto();
        event.setUserId(2L);
        event.setRole("PASSENGER");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }
}
