package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.OrderStatusChangedEventDto;
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
class OrderStatusChangedListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private OrderStatusChangedListener listener;

    @Test
    void onMessage_Completed() {
        OrderStatusChangedEventDto event = new OrderStatusChangedEventDto();
        event.setOrderId(1L);
        event.setNewStatus("COMPLETED");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }

    @Test
    void onMessage_Cancelled() {
        OrderStatusChangedEventDto event = new OrderStatusChangedEventDto();
        event.setOrderId(1L);
        event.setNewStatus("CANCELLED");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }

    @Test
    void onMessage_Matched() {
        OrderStatusChangedEventDto event = new OrderStatusChangedEventDto();
        event.setOrderId(1L);
        event.setNewStatus("MATCHED");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }
}
