package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.UserRegisteredEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private NotificationDispatcherService dispatcherService;

    @InjectMocks
    private UserEventListener listener;

    @Test
    void onMessage_ShouldDispatch() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(1L);

        listener.onMessage(event);

        verify(dispatcherService).dispatchUserRegistered(event);
    }
}
