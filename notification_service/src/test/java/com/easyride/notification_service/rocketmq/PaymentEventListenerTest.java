package com.easyride.notification_service.rocketmq;

import com.easyride.notification_service.dto.PaymentSuccessEvent;
import com.easyride.notification_service.service.NotificationDispatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private NotificationDispatcherService dispatcherService;

    @InjectMocks
    private PaymentEventListener listener;

    @Test
    void onMessage_ShouldDispatch() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setPaymentId(1L);

        listener.onMessage(event);

        verify(dispatcherService).dispatchPaymentSuccess(event);
    }
}
