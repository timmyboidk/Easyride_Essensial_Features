package com.easyride.analytics_service.rocket;

import com.easyride.analytics_service.dto.AdminLogEvent;
import com.easyride.analytics_service.dto.AnalyticsRequestDto;
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
class AdminLogEventListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AdminLogEventListener listener;

    @Test
    void onMessage_Success() {
        AdminLogEvent event = new AdminLogEvent();
        event.setAdminId(1L);
        event.setOperationType("CREATE");
        event.setTimestamp(LocalDateTime.now());

        listener.onMessage(event);

        verify(analyticsService, times(1)).recordAnalyticsData(any(AnalyticsRequestDto.class));
    }
}
