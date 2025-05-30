package com.easyride.analytics_service;

import com.easyride.analytics_service.service.AnalyticsServiceImpl;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsRepository;
import com.easyride.analytics_service.util.PrivacyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void recordAnalyticsData_ShouldCallPrivacyUtilAndSave() {
        // 1. 构造一个 AnalyticsRecord
        AnalyticsRecord record = AnalyticsRecord.builder()
            .recordType(RecordType.ORDER_DATA)
            .metricName("orderRevenue")
            .metricValue(888.88)
            .recordTime(LocalDateTime.now())
            .dimensionKey("email")
            .dimensionValue("test@example.com")
            .build();

        // 2. 执行 recordAnalyticsData
        analyticsService.recordAnalyticsData(record);

        // 3. 验证 repository.save() 被调用
        ArgumentCaptor<AnalyticsRecord> captor = ArgumentCaptor.forClass(AnalyticsRecord.class);
        verify(analyticsRepository).save(captor.capture());
        AnalyticsRecord savedRecord = captor.getValue();

        // 4. 验证已进行脱敏
        // 如果 dimensionKey 是 email，那么 dimensionValue 应该被替换为脱敏形式
        // 这部分逻辑由 PrivacyUtil 决定，假设 test@example.com -> t**@example.com
        assertNotEquals("test@example.com", savedRecord.getDimensionValue(), "Email should be masked");

        // 也可以检查 savedRecord 的其他字段值是否保持一致
        assertEquals("orderRevenue", savedRecord.getMetricName());
        assertEquals(888.88, savedRecord.getMetricValue());
        assertEquals(RecordType.ORDER_DATA, savedRecord.getRecordType());
    }
}
