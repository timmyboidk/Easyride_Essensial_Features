package com.easyride.analytics_service;

import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsRepository;
import com.easyride.analytics_service.service.AnalyticsServiceImpl;
import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.dto.DashboardSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HyperLogLogOperations<String, Object> hyperLogLogOperations;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setup() {
        // Mock Redis opsForHyperLogLog if needed by tests
        // leniency needed because not all tests use redis
        lenient().when(redisTemplate.opsForHyperLogLog()).thenReturn(hyperLogLogOperations);
    }

    @Test
    void recordAnalyticsData_ShouldSaveRecord_WhenNotActiveUserLogin() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("key", "value");
        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .recordType(RecordType.ORDER_REVENUE.name())
                .metricName("revenue")
                .metricValue(100.0)
                .recordTime(LocalDateTime.now())
                .dimensions(dimensions)
                .build();

        analyticsService.recordAnalyticsData(request);

        verify(analyticsRepository, times(1)).save(any(AnalyticsRecord.class));
    }

    @Test
    void recordAnalyticsData_ShouldUseRedis_WhenActiveUserLogin() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("userId", "user123");
        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                .recordTime(LocalDateTime.now())
                .dimensions(dimensions)
                .build();

        analyticsService.recordAnalyticsData(request);

        verify(hyperLogLogOperations, times(1)).add(contains("dau:"), eq("user123"));
        verify(hyperLogLogOperations, times(1)).add(contains("mau:"), eq("user123"));
        verify(analyticsRepository, never()).save(any(AnalyticsRecord.class));
    }

    @Test
    void getDailyActiveUsers_ShouldReturnCount() {
        String dateStr = "2023-10-01";
        when(hyperLogLogOperations.size("dau:" + dateStr)).thenReturn(50L);

        long dau = analyticsService.getDailyActiveUsers(dateStr);

        assertEquals(50L, dau);
    }

    @Test
    void getMonthlyActiveUsers_ShouldReturnCount() {
        String monthStr = "2023-10";
        when(hyperLogLogOperations.size("mau:" + monthStr)).thenReturn(1000L);

        long mau = analyticsService.getMonthlyActiveUsers(monthStr);

        assertEquals(1000L, mau);
    }

    @Test
    void getAverageOrderValue_ShouldCalculateCorrectly() {
        String start = "2023-10-01";
        String end = "2023-10-02";

        AnalyticsRecord revenueRecord = new AnalyticsRecord();
        revenueRecord.setMetricValue(500.0);

        AnalyticsRecord countRecord = new AnalyticsRecord();
        countRecord.setMetricValue(5.0); // Assuming 5 completed orders

        when(analyticsRepository.findByRecordTypeAndRecordTimeBetween(eq(RecordType.ORDER_REVENUE), any(), any()))
                .thenReturn(Collections.singletonList(revenueRecord));
        when(analyticsRepository.findByRecordTypeAndRecordTimeBetween(eq(RecordType.COMPLETED_ORDERS_COUNT), any(),
                any()))
                .thenReturn(Collections.singletonList(countRecord));

        double aov = analyticsService.getAverageOrderValue(start, end);

        assertEquals(100.0, aov);
    }

    @Test
    void getDriverAcceptanceRate_ShouldCalculateCorrectly() {
        String start = "2023-10-01";
        String end = "2023-10-02";

        when(analyticsRepository.countByRecordTypeAndRecordTimeBetween(eq(RecordType.ORDER_REQUEST), any(), any()))
                .thenReturn(10L);
        when(analyticsRepository.countByRecordTypeAndRecordTimeBetween(eq(RecordType.ORDER_ACCEPTED_BY_DRIVER), any(),
                any()))
                .thenReturn(8L);

        double rate = analyticsService.getDriverAcceptanceRate(start, end);

        assertEquals(80.0, rate);
    }

    @Test
    void getUserRetentionRate_ShouldReturnZero_WhenNoCohortUsers() {
        String cohortMonth = "2023-01";
        when(analyticsRepository.findByRecordTypeAndRecordTimeBetween(eq(RecordType.USER_REGISTRATION), any(), any()))
                .thenReturn(Collections.emptyList());

        double retention = analyticsService.getUserRetentionRate(cohortMonth, 1);

        assertEquals(0.0, retention);
    }

    @Test
    void getAdminDashboardSummary_ShouldReturnSummary() {
        when(analyticsRepository.countByRecordTypeAndRecordTimeBetween(eq(RecordType.COMPLETED_ORDERS_COUNT), any(),
                any()))
                .thenReturn(100L);
        when(analyticsRepository.findByRecordTypeAndRecordTimeBetween(eq(RecordType.ORDER_REVENUE), any(), any()))
                .thenReturn(List.of(AnalyticsRecord.builder().metricValue(2000.0).build()));
        when(analyticsRepository.countByRecordTypeAndRecordTimeBetween(eq(RecordType.USER_REGISTRATION), any(), any()))
                .thenReturn(50L);

        DashboardSummaryDto summary = analyticsService.getAdminDashboardSummary("TODAY");

        assertEquals(100L, summary.getTotalOrders());
        assertEquals(2000.0, summary.getTotalRevenue());
        assertEquals(50L, summary.getNewUsers());
    }
}
