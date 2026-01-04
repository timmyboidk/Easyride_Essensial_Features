package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.OrderCreatedEvent;
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
class OrderCreatedListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private OrderCreatedListener listener;

    @Test
    void onMessage_Success() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);
        event.setPassengerId(2L);
        event.setServiceType("NORMAL");
        event.setCreatedTime(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }
}
