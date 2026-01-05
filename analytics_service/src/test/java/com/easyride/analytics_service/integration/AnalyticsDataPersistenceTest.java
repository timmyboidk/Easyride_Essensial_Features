package com.easyride.analytics_service.integration;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsMapper;
import com.easyride.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for analytics data persistence.
 * 
 * Tests verify that:
 * 1. Analytics records are correctly persisted to MySQL
 * 2. DAU/MAU tracking works with real Redis HyperLogLog
 * 3. Aggregation queries return correct results from real data
 */
@DisplayName("Analytics Data Persistence Integration Tests")
class AnalyticsDataPersistenceTest extends AnalyticsIntegrationTestBase {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsMapper analyticsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        analyticsMapper.delete(null);

        // Clean up Redis keys
        String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String monthKey = "mau:" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        redisTemplate.delete(todayKey);
        redisTemplate.delete(monthKey);
    }

    @Test
    @DisplayName("Should persist ORDER_REVENUE record to MySQL")
    void testRecordOrderRevenue_PersistsToMySQL() {
        // Arrange
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("orderId", "order-12345");
        dimensions.put("region", "Beijing");

        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .recordType(RecordType.ORDER_REVENUE.name())
                .metricName("revenue")
                .metricValue(150.50)
                .recordTime(LocalDateTime.now())
                .dimensions(dimensions)
                .build();

        // Act
        analyticsService.recordAnalyticsData(request);

        // Assert - verify data is in MySQL
        Long count = analyticsMapper.selectCount(null);
        assertTrue(count >= 1, "At least one record should be persisted");

        // Verify the content
        var records = analyticsMapper.selectList(null);
        boolean foundRevenueRecord = records.stream()
                .anyMatch(r -> r.getRecordType() == RecordType.ORDER_REVENUE
                        && r.getMetricValue() != null
                        && r.getMetricValue() == 150.50);
        assertTrue(foundRevenueRecord, "ORDER_REVENUE record with correct value should exist");
    }

    @Test
    @DisplayName("Should track ACTIVE_USER_LOGIN in Redis HyperLogLog for DAU/MAU")
    void testRecordActiveUserLogin_TracksInRedisHyperLogLog() {
        // Arrange
        String userId = "user-uuid-12345";
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("userId", userId);

        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                .recordTime(LocalDateTime.now())
                .dimensions(dimensions)
                .build();

        // Act
        analyticsService.recordAnalyticsData(request);

        // Assert - verify user is tracked in Redis HyperLogLog
        String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String monthKey = "mau:" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Long dauCount = redisTemplate.opsForHyperLogLog().size(todayKey);
        Long mauCount = redisTemplate.opsForHyperLogLog().size(monthKey);

        assertNotNull(dauCount);
        assertNotNull(mauCount);
        assertEquals(1L, dauCount, "DAU should be 1 after single user login");
        assertEquals(1L, mauCount, "MAU should be 1 after single user login");

        // Verify that duplicate user login doesn't increase count (HyperLogLog
        // deduplication)
        analyticsService.recordAnalyticsData(request);
        Long dauCountAfterDupe = redisTemplate.opsForHyperLogLog().size(todayKey);
        assertEquals(1L, dauCountAfterDupe, "DAU should still be 1 after duplicate login");
    }

    @Test
    @DisplayName("Should return correct DAU count from Redis")
    void testGetDailyActiveUsers_ReturnsCorrectCount() {
        // Arrange - add multiple unique users
        for (int i = 0; i < 10; i++) {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("userId", "user-" + i);

            AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                    .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                    .recordTime(LocalDateTime.now())
                    .dimensions(dimensions)
                    .build();

            analyticsService.recordAnalyticsData(request);
        }

        // Act
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        long dau = analyticsService.getDailyActiveUsers(today);

        // Assert - HyperLogLog has ~0.81% standard error, so for small cardinalities
        // the count may be slightly off. Allow +/- 1 for counts under 20.
        assertTrue(dau >= 9 && dau <= 11,
                "Should have approximately 10 unique daily active users (got: " + dau + ")");
    }

    @Test
    @DisplayName("Should return correct MAU count from Redis")
    void testGetMonthlyActiveUsers_ReturnsCorrectCount() {
        // Arrange - add multiple unique users
        for (int i = 0; i < 25; i++) {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("userId", "monthly-user-" + i);

            AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                    .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                    .recordTime(LocalDateTime.now())
                    .dimensions(dimensions)
                    .build();

            analyticsService.recordAnalyticsData(request);
        }

        // Act
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long mau = analyticsService.getMonthlyActiveUsers(currentMonth);

        // Assert - HyperLogLog has ~0.81% standard error, allow small margin
        assertTrue(mau >= 24 && mau <= 26,
                "Should have approximately 25 unique monthly active users (got: " + mau + ")");
    }

    @Test
    @DisplayName("Should calculate correct average order value from persisted data")
    void testGetAverageOrderValue_CalculatesCorrectly() {
        // Arrange - insert revenue and order count records
        LocalDateTime now = LocalDateTime.now();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

        // Insert 3 revenue records totaling 300.0
        for (int i = 0; i < 3; i++) {
            AnalyticsRecord revenueRecord = AnalyticsRecord.builder()
                    .recordType(RecordType.ORDER_REVENUE)
                    .metricName("revenue")
                    .metricValue(100.0) // Each order = 100.0
                    .recordTime(now)
                    .build();
            analyticsMapper.insert(revenueRecord);
        }

        // Insert 1 order count record representing 3 completed orders
        AnalyticsRecord countRecord = AnalyticsRecord.builder()
                .recordType(RecordType.COMPLETED_ORDERS_COUNT)
                .metricName("completed_orders")
                .metricValue(3.0)
                .recordTime(now)
                .build();
        analyticsMapper.insert(countRecord);

        // Act
        double aov = analyticsService.getAverageOrderValue(today, tomorrow);

        // Assert - 300 total revenue / 3 orders = 100.0 AOV
        assertEquals(100.0, aov, 0.01, "Average order value should be 100.0");
    }

    @Test
    @DisplayName("Should calculate correct driver acceptance rate from persisted data")
    void testGetDriverAcceptanceRate_CalculatesCorrectly() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

        // Insert 10 order request records
        for (int i = 0; i < 10; i++) {
            AnalyticsRecord requestRecord = AnalyticsRecord.builder()
                    .recordType(RecordType.ORDER_REQUEST)
                    .metricName("order_request")
                    .metricValue(1.0)
                    .recordTime(now)
                    .build();
            analyticsMapper.insert(requestRecord);
        }

        // Insert 8 order accepted records (80% acceptance rate)
        for (int i = 0; i < 8; i++) {
            AnalyticsRecord acceptedRecord = AnalyticsRecord.builder()
                    .recordType(RecordType.ORDER_ACCEPTED_BY_DRIVER)
                    .metricName("order_accepted")
                    .metricValue(1.0)
                    .recordTime(now)
                    .build();
            analyticsMapper.insert(acceptedRecord);
        }

        // Act
        double rate = analyticsService.getDriverAcceptanceRate(today, tomorrow);

        // Assert - 8/10 = 80%
        assertEquals(80.0, rate, 0.01, "Driver acceptance rate should be 80%");
    }

    @Test
    @DisplayName("Should return correct admin dashboard summary from persisted data")
    void testGetAdminDashboardSummary_ReturnsAggregatedData() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Insert completed orders
        for (int i = 0; i < 5; i++) {
            AnalyticsRecord orderRecord = AnalyticsRecord.builder()
                    .recordType(RecordType.COMPLETED_ORDERS_COUNT)
                    .metricName("completed_order")
                    .metricValue(1.0)
                    .recordTime(now)
                    .build();
            analyticsMapper.insert(orderRecord);
        }

        // Insert revenue
        AnalyticsRecord revenueRecord = AnalyticsRecord.builder()
                .recordType(RecordType.ORDER_REVENUE)
                .metricName("revenue")
                .metricValue(500.0)
                .recordTime(now)
                .build();
        analyticsMapper.insert(revenueRecord);

        // Insert new user registrations
        for (int i = 0; i < 3; i++) {
            AnalyticsRecord userRecord = AnalyticsRecord.builder()
                    .recordType(RecordType.USER_REGISTRATION)
                    .metricName("new_user")
                    .metricValue(1.0)
                    .recordTime(now)
                    .build();
            analyticsMapper.insert(userRecord);
        }

        // Act
        var summary = analyticsService.getAdminDashboardSummary("TODAY");

        // Assert
        assertNotNull(summary, "Dashboard summary should not be null");
        assertEquals(5L, summary.getTotalOrders(), "Should have 5 total orders");
        assertEquals(500.0, summary.getTotalRevenue(), 0.01, "Total revenue should be 500.0");
        assertEquals(3L, summary.getNewUsers(), "Should have 3 new users");
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void testEmptyData_ReturnsZeroValues() {
        // Act
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

        long dau = analyticsService.getDailyActiveUsers(today);
        double aov = analyticsService.getAverageOrderValue(today, tomorrow);
        double acceptanceRate = analyticsService.getDriverAcceptanceRate(today, tomorrow);

        // Assert
        assertEquals(0L, dau, "DAU should be 0 with no data");
        assertEquals(0.0, aov, 0.01, "AOV should be 0 with no data");
        assertEquals(0.0, acceptanceRate, 0.01, "Acceptance rate should be 0 with no data");
    }
}
