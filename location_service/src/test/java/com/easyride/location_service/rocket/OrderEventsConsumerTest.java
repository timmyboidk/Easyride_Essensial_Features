package com.easyride.location_service.rocket;

import com.easyride.location_service.dto.OrderStartedEventDto;
import com.easyride.location_service.dto.OrderTerminatedEventDto;
import com.easyride.location_service.model.PlannedRoute;
import com.easyride.location_service.service.SafetyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventsConsumerTest {

    @Mock
    private SafetyService safetyService;

    @InjectMocks
    private OrderEventsConsumer consumer;

    @Test
    void onMessage_OrderStarted_WithRoute() {
        OrderStartedEventDto event = new OrderStartedEventDto();
        event.setOrderId(1L);
        event.setPlannedRoute(new PlannedRoute());

        consumer.onMessage(event);

        verify(safetyService).storePlannedRoute(any(PlannedRoute.class));
    }

    @Test
    void onMessage_OrderStarted_NoRoute() {
        OrderStartedEventDto event = new OrderStartedEventDto();
        event.setOrderId(1L);
        event.setPlannedRoute(null);

        consumer.onMessage(event);

        verify(safetyService, never()).storePlannedRoute(any());
    }

    @Test
    void onMessage_OrderTerminated() {
        OrderTerminatedEventDto event = new OrderTerminatedEventDto();
        event.setOrderId(1L);

        consumer.onMessage(event);

        verify(safetyService).removePlannedRoute(1L);
    }

    @Test
    void onMessage_UnknownType() {
        consumer.onMessage(new Object());
        verifyNoInteractions(safetyService);
    }
}
