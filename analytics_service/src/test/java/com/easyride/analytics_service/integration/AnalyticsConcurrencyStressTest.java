package com.easyride.analytics_service.integration;

import com.easyride.analytics_service.dto.AnalyticsRequestDto;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsMapper;
import com.easyride.analytics_service.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * High concurrency stress tests for analytics service.
 * 
 * Tests verify:
 * 1. Data integrity under concurrent writes
 * 2. No data loss with high-throughput recording
 * 3. System stability under load
 * 4. Performance metrics (throughput, latency)
 */
@DisplayName("Analytics High Concurrency Stress Tests")
class AnalyticsConcurrencyStressTest extends AnalyticsIntegrationTestBase {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsMapper analyticsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("Concurrent ORDER_REVENUE recording - All data should be persisted")
    void testConcurrentAnalyticsRecording_AllDataPersisted() throws InterruptedException {
        // Arrange
        int threadCount = 50;
        int recordsPerThread = 10;
        int totalExpectedRecords = threadCount * recordsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Act
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int r = 0; r < recordsPerThread; r++) {
                        Map<String, String> dimensions = new HashMap<>();
                        dimensions.put("orderId", "order-" + threadId + "-" + r);
                        dimensions.put("threadId", String.valueOf(threadId));

                        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                .recordType(RecordType.ORDER_REVENUE.name())
                                .metricName("concurrent_revenue")
                                .metricValue(100.0 + threadId)
                                .recordTime(LocalDateTime.now())
                                .dimensions(dimensions)
                                .build();

                        analyticsService.recordAnalyticsData(request);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion with timeout
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(completed, "All threads should complete within timeout");

        // Note: Due to dimension iteration in service, actual records may be 2x (2
        // dimensions per request)
        Long persistedCount = analyticsMapper.selectCount(null);
        System.out.println("=== Concurrent Recording Results ===");
        System.out.println("Expected records (min): " + totalExpectedRecords);
        System.out.println("Persisted records: " + persistedCount);
        System.out.println("Success count: " + successCount.get());
        System.out.println("Failure count: " + failureCount.get());

        assertTrue(persistedCount >= totalExpectedRecords,
                "All records should be persisted. Expected at least: " + totalExpectedRecords + ", got: "
                        + persistedCount);
        assertEquals(0, failureCount.get(), "No failures should occur");
    }

    @Test
    @DisplayName("Concurrent DAU tracking - No data loss with HyperLogLog")
    void testConcurrentDauTracking_NoDataLoss() throws InterruptedException {
        // Arrange
        int threadCount = 100;
        int usersPerThread = 10;
        int totalUniqueUsers = threadCount * usersPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int u = 0; u < usersPerThread; u++) {
                        Map<String, String> dimensions = new HashMap<>();
                        // Each user is unique across all threads
                        dimensions.put("userId", "user-" + threadId + "-" + u);

                        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                                .recordTime(LocalDateTime.now())
                                .dimensions(dimensions)
                                .build();

                        analyticsService.recordAnalyticsData(request);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(completed, "All threads should complete within timeout");

        String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Long dauCount = redisTemplate.opsForHyperLogLog().size(todayKey);

        System.out.println("=== Concurrent DAU Tracking Results ===");
        System.out.println("Total unique users expected: " + totalUniqueUsers);
        System.out.println("DAU count from HyperLogLog: " + dauCount);
        System.out.println("Success count: " + successCount.get());

        // HyperLogLog has ~0.81% error rate, so allow some margin
        double errorMargin = 0.02; // 2% margin
        long minExpected = (long) (totalUniqueUsers * (1 - errorMargin));
        long maxExpected = (long) (totalUniqueUsers * (1 + errorMargin));

        assertTrue(dauCount >= minExpected && dauCount <= maxExpected,
                "DAU count should be within acceptable HyperLogLog error margin. Expected: " +
                        minExpected + "-" + maxExpected + ", got: " + dauCount);
    }

    @Test
    @DisplayName("High throughput HTTP recording - Measure performance")
    void testHighThroughputRecording_MeasurePerformance() throws Exception {
        // Arrange
        int totalRequests = 200;
        List<Long> latencies = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Act - measure throughput
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            Map<String, String> dimensions = new HashMap<>();
            dimensions.put("requestId", "perf-test-" + i);

            AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                    .recordType(RecordType.ORDER_REVENUE.name())
                    .metricName("performance_test")
                    .metricValue(50.0 + (i % 100))
                    .recordTime(LocalDateTime.now())
                    .dimensions(dimensions)
                    .build();

            long reqStart = System.nanoTime();
            try {
                mockMvc.perform(post("/analytics/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
            long reqEnd = System.nanoTime();
            latencies.add((reqEnd - reqStart) / 1_000_000); // Convert to ms
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calculate metrics
        double throughput = (double) successCount.get() / (totalTime / 1000.0);
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
        long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);

        // Calculate p95 and p99
        latencies.sort(Long::compareTo);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        // Assert & Report
        System.out.println("=== High Throughput Performance Results ===");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println("Avg latency: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("Min latency: " + minLatency + " ms");
        System.out.println("Max latency: " + maxLatency + " ms");
        System.out.println("P95 latency: " + p95 + " ms");
        System.out.println("P99 latency: " + p99 + " ms");

        assertEquals(totalRequests, successCount.get(), "All requests should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
        assertTrue(avgLatency < 500, "Average latency should be under 500ms");
    }

    @Test
    @DisplayName("Mixed concurrent read/write operations - System stability")
    void testMixedConcurrentOperations_SystemStability() throws InterruptedException {
        // Arrange
        int writerThreads = 20;
        int readerThreads = 10;
        int operationsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(writerThreads + readerThreads);
        AtomicInteger writeSuccess = new AtomicInteger(0);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        // Pre-populate some data
        String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        for (int i = 0; i < 50; i++) {
            redisTemplate.opsForHyperLogLog().add(todayKey, "prep-user-" + i);
        }

        // Writer threads - insert ORDER_REVENUE records
        for (int t = 0; t < writerThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < operationsPerThread; i++) {
                        Map<String, String> dimensions = new HashMap<>();
                        dimensions.put("mixedTest", "writer-" + threadId + "-" + i);

                        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                                .recordType(RecordType.ORDER_REVENUE.name())
                                .metricName("mixed_test")
                                .metricValue(75.0)
                                .recordTime(LocalDateTime.now())
                                .dimensions(dimensions)
                                .build();

                        analyticsService.recordAnalyticsData(request);
                        writeSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Reader threads - query DAU
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                    for (int i = 0; i < operationsPerThread; i++) {
                        long dau = analyticsService.getDailyActiveUsers(today);
                        if (dau >= 0) {
                            readSuccess.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Execute
        startLatch.countDown();
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        System.out.println("=== Mixed Concurrent Operations Results ===");
        System.out.println("Writer threads: " + writerThreads);
        System.out.println("Reader threads: " + readerThreads);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Write successes: " + writeSuccess.get());
        System.out.println("Read successes: " + readSuccess.get());
        System.out.println("Errors: " + errors.get());

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(writerThreads * operationsPerThread, writeSuccess.get(),
                "All write operations should succeed");
        assertEquals(readerThreads * operationsPerThread, readSuccess.get(),
                "All read operations should succeed");
        assertEquals(0, errors.get(), "No errors should occur");
    }

    @Test
    @DisplayName("Burst load - Handle sudden spike in traffic")
    void testBurstLoad_HandleSuddenSpike() throws InterruptedException {
        // Arrange
        int burstSize = 100;
        ExecutorService executor = Executors.newFixedThreadPool(burstSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(burstSize);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        // Act - fire all requests simultaneously
        for (int i = 0; i < burstSize; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    long start = System.currentTimeMillis();

                    Map<String, String> dimensions = new HashMap<>();
                    dimensions.put("userId", "burst-user-" + requestId);

                    AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                            .recordType(RecordType.ACTIVE_USER_LOGIN.name())
                            .recordTime(LocalDateTime.now())
                            .dimensions(dimensions)
                            .build();

                    analyticsService.recordAnalyticsData(request);

                    long latency = System.currentTimeMillis() - start;
                    totalLatency.addAndGet(latency);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        long burstStartTime = System.currentTimeMillis();
        startLatch.countDown();

        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        long burstEndTime = System.currentTimeMillis();
        executor.shutdown();

        // Assert
        long totalTime = burstEndTime - burstStartTime;
        double avgLatency = (double) totalLatency.get() / successCount.get();

        System.out.println("=== Burst Load Results ===");
        System.out.println("Burst size: " + burstSize);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Total burst handling time: " + totalTime + " ms");
        System.out.println("Average request latency: " + String.format("%.2f", avgLatency) + " ms");

        assertTrue(completed, "All burst requests should complete within timeout");
        assertEquals(burstSize, successCount.get(), "All burst requests should succeed");

        // Verify all users were tracked
        String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Long dauCount = redisTemplate.opsForHyperLogLog().size(todayKey);

        // Allow for HyperLogLog margin
        assertTrue(dauCount >= burstSize * 0.98,
                "DAU should reflect burst users. Expected ~" + burstSize + ", got: " + dauCount);
    }
}
