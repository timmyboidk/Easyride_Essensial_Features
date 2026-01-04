package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.OrderCreatedEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    @Mock
    private NotificationDispatcherService dispatcherService;

    @InjectMocks
    private OrderCreatedListener listener;

    @Test
    void onMessage_ShouldDispatch() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(1L);

        listener.onMessage(event);

        verify(dispatcherService).dispatchOrderCreated(event);
    }
}
