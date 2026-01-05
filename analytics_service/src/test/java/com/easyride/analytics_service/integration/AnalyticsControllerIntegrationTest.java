package com.easyride.analytics_service.integration;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP endpoint integration tests for Analytics Controller.
 * 
 * Tests verify that:
 * 1. REST endpoints accept valid requests and return proper responses
 * 2. Data flows correctly from HTTP request to database/Redis
 * 3. Error handling works correctly for invalid inputs
 */
@DisplayName("Analytics Controller HTTP Integration Tests")
class AnalyticsControllerIntegrationTest extends AnalyticsIntegrationTestBase {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private AnalyticsMapper analyticsMapper;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        @BeforeEach
        void setUp() {
                // Clean up database
                analyticsMapper.delete(null);

                // Clean up Redis keys
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String monthKey = "mau:" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                redisTemplate.delete(todayKey);
                redisTemplate.delete(monthKey);
        }

        @Test
        @DisplayName("POST /analytics/record - Should return 200 and persist ORDER_REVENUE data")
        void testRecordAnalytics_OrderRevenue_ShouldReturn200AndPersist() throws Exception {
                // Arrange
                Map<String, String> dimensions = new HashMap<>();
                dimensions.put("orderId", "order-http-test-123");
                dimensions.put("region", "Shanghai");

                AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                .recordType(RecordType.ORDER_REVENUE.name())
                                .metricName("revenue")
                                .metricValue(250.75)
                                .recordTime(LocalDateTime.now())
                                .dimensions(dimensions)
                                .build();

                // Act & Assert
                mockMvc.perform(post("/analytics/record")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.message", containsString("成功")));

                // Verify data persistence
                Long count = analyticsMapper.selectCount(null);
                assertTrue(count >= 1, "Record should be persisted to database");
        }

        @Test
        @DisplayName("POST /analytics/record - Should track active user login in Redis")
        void testRecordAnalytics_ActiveUserLogin_ShouldTrackInRedis() throws Exception {
                // Arrange
                Map<String, String> dimensions = new HashMap<>();
                dimensions.put("userId", "http-test-user-456");

                AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                                .recordTime(LocalDateTime.now())
                                .dimensions(dimensions)
                                .build();

                // Act
                mockMvc.perform(post("/analytics/record")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)));

                // Assert - verify Redis tracking
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                Long dauCount = redisTemplate.opsForHyperLogLog().size(todayKey);
                assertEquals(1L, dauCount, "DAU should be tracked in Redis");
        }

        @Test
        @DisplayName("GET /analytics/metrics/dau - Should return correct DAU count")
        void testGetDailyActiveUsers_ShouldReturnCorrectCount() throws Exception {
                // Arrange - pre-populate Redis with users via service
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                redisTemplate.opsForHyperLogLog().add(todayKey, "user1", "user2", "user3", "user4", "user5");

                String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

                // Act & Assert
                mockMvc.perform(get("/analytics/metrics/dau")
                                .param("date", today))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(5)));
        }

        @Test
        @DisplayName("GET /analytics/metrics/mau - Should return correct MAU count")
        void testGetMonthlyActiveUsers_ShouldReturnCorrectCount() throws Exception {
                // Arrange - pre-populate Redis with users
                String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                String mauKey = "mau:" + currentMonth;

                for (int i = 0; i < 15; i++) {
                        redisTemplate.opsForHyperLogLog().add(mauKey, "monthly-user-" + i);
                }

                // Act & Assert
                mockMvc.perform(get("/analytics/metrics/mau")
                                .param("yearMonth", currentMonth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(15)));
        }

        @Test
        @DisplayName("GET /analytics/metrics/average-order-value - Should calculate and return AOV")
        void testGetAverageOrderValue_ShouldCalculateAndReturn() throws Exception {
                // Arrange - insert test data
                LocalDateTime now = LocalDateTime.now();

                // Insert revenue records (total: 400.0)
                for (double revenue : new double[] { 100.0, 150.0, 150.0 }) {
                        AnalyticsRecord revenueRecord = AnalyticsRecord.builder()
                                        .recordType(RecordType.ORDER_REVENUE)
                                        .metricName("revenue")
                                        .metricValue(revenue)
                                        .recordTime(now)
                                        .build();
                        analyticsMapper.insert(revenueRecord);
                }

                // Insert order count record (4 orders)
                AnalyticsRecord countRecord = AnalyticsRecord.builder()
                                .recordType(RecordType.COMPLETED_ORDERS_COUNT)
                                .metricName("completed_orders")
                                .metricValue(4.0)
                                .recordTime(now)
                                .build();
                analyticsMapper.insert(countRecord);

                String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

                // Act & Assert - 400.0 / 4 = 100.0
                mockMvc.perform(get("/analytics/metrics/average-order-value")
                                .param("startDate", today)
                                .param("endDate", tomorrow))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(100.0)));
        }

        @Test
        @DisplayName("GET /analytics/metrics/dau - Should return 0 for empty data")
        void testGetDailyActiveUsers_EmptyData_ShouldReturnZero() throws Exception {
                String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

                mockMvc.perform(get("/analytics/metrics/dau")
                                .param("date", today))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(0)));
        }

        @Test
        @DisplayName("GET /analytics/metrics/average-order-value - Should return 0 when no orders")
        void testGetAverageOrderValue_NoOrders_ShouldReturnZero() throws Exception {
                String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

                mockMvc.perform(get("/analytics/metrics/average-order-value")
                                .param("startDate", today)
                                .param("endDate", tomorrow))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(0.0)));
        }

        @Test
        @DisplayName("Multiple sequential requests should all succeed")
        void testMultipleSequentialRequests_AllShouldSucceed() throws Exception {
                // Arrange
                Map<String, String> dimensions = new HashMap<>();

                // Act - send multiple requests sequentially
                for (int i = 0; i < 5; i++) {
                        dimensions.put("userId", "sequential-user-" + i);

                        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                        .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                                        .recordTime(LocalDateTime.now())
                                        .dimensions(new HashMap<>(dimensions))
                                        .build();

                        mockMvc.perform(post("/analytics/record")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());
                }

                // Assert - verify all tracked
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                Long dauCount = redisTemplate.opsForHyperLogLog().size(todayKey);
                assertEquals(5L, dauCount, "All 5 unique users should be tracked");
        }

        @Test
        @DisplayName("Full flow: Record data -> Query metrics")
        void testFullFlow_RecordAndQueryMetrics() throws Exception {
                // Arrange & Act - Step 1: Record multiple user logins
                for (int i = 0; i < 10; i++) {
                        Map<String, String> dimensions = new HashMap<>();
                        dimensions.put("userId", "flow-test-user-" + i);

                        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                        .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                                        .recordTime(LocalDateTime.now())
                                        .dimensions(dimensions)
                                        .build();

                        mockMvc.perform(post("/analytics/record")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());
                }

                // Act - Step 2: Query the DAU
                String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

                MvcResult result = mockMvc.perform(get("/analytics/metrics/dau")
                                .param("date", today))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(0)))
                                .andExpect(jsonPath("$.data", is(10)))
                                .andReturn();

                // Assert
                String responseBody = result.getResponse().getContentAsString();
                assertTrue(responseBody.contains("\"data\":10"), "Response should contain data:10");
        }
}
