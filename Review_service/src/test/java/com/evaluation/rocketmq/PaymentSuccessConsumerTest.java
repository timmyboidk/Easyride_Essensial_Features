package com.evaluation.rocketmq;

import com.evaluation.dto.PaymentSuccessEvent;
import com.evaluation.service.ReviewWindowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessConsumerTest {

    @Mock
    private ReviewWindowService reviewWindowService;

    @InjectMocks
    private PaymentSuccessConsumer paymentSuccessConsumer;

    @Test
    void onMessage_Success() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setOrderId(123L);
        event.setAmount(BigDecimal.TEN);
        event.setPaymentTime(LocalDateTime.now());

        paymentSuccessConsumer.onMessage(event);

        verify(reviewWindowService).openReviewWindow(eq(123L), any(), any(), eq(event.getPaymentTime()));
    }
}
