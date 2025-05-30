package rocketmq;

import com.easyride.analytics_service.dto.OrderCompletedEvent;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.rocket.OrderEventListener;
import com.easyride.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class OrderEventListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void onMessage_ShouldRecordOrderCompletedData() {
        // 1. 构造一个 OrderCompletedEvent
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setOrderId(123L);
        event.setPassengerId(45L);
        event.setOrderAmount(99.99);
        event.setRegion("East");
        event.setCompletedTime(LocalDateTime.of(2023, 9, 10, 15, 30));

        // 2. 执行 onMessage
        orderEventListener.onMessage(event);

        // 3. 验证 analyticsService.recordAnalyticsData 被正确调用了两次（orderRevenue, completedOrderCount）
        ArgumentCaptor<AnalyticsRecord> captor = ArgumentCaptor.forClass(AnalyticsRecord.class);
        verify(analyticsService, times(2)).recordAnalyticsData(captor.capture());

        // 4. 捕获并检查对应的 AnalyticsRecord
        //    第一次捕获的是 "orderRevenue"
        //    第二次捕获的是 "completedOrderCount"
        AnalyticsRecord firstRecord = captor.getAllValues().get(0);
        assertEquals(RecordType.ORDER_DATA, firstRecord.getRecordType());
        assertEquals("orderRevenue", firstRecord.getMetricName());
        assertEquals(99.99, firstRecord.getMetricValue());
        assertEquals("region", firstRecord.getDimensionKey());
        assertEquals("East", firstRecord.getDimensionValue());

        AnalyticsRecord secondRecord = captor.getAllValues().get(1);
        assertEquals("completedOrderCount", secondRecord.getMetricName());
        assertEquals(1.0, secondRecord.getMetricValue());
    }
}
