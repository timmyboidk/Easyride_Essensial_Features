package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.OrderStatusChangedEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderStatusChangedListenerTest {

    @Mock
    private NotificationDispatcherService dispatcherService;

    @InjectMocks
    private OrderStatusChangedListener listener;

    @Test
    void onMessage_ShouldDispatch() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent();
        event.setOrderId(1L);

        listener.onMessage(event);

        verify(dispatcherService).dispatchOrderStatusChanged(event);
    }
}
