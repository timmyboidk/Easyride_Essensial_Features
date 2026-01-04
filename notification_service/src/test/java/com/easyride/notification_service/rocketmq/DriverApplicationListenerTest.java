package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.DriverApplicationEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverApplicationListenerTest {

    @Mock
    private NotificationDispatcherService dispatcherService;

    @InjectMocks
    private DriverApplicationListener listener;

    @Test
    void onMessage_ShouldDispatch() {
        DriverApplicationEvent event = new DriverApplicationEvent();
        event.setDriverUserId(1L);

        listener.onMessage(event);

        verify(dispatcherService).dispatchDriverApplication(event);
    }
}
