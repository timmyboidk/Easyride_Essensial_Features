package com.easyride.analytics_service.service;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsRepository;
import com.easyride.analytics_service.util.PrivacyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate; // For HyperLogLog (DAU/MAU)
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);
    private final AnalyticsRepository analyticsRepository;
    private final RedisTemplate<String, Object> redisTemplate;// For unique user tracking with HLL or Sets

    @Autowired
    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository, RedisTemplate<String, Object> redisTemplate) {
        this.analyticsRepository = analyticsRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordAnalyticsData(AnalyticsRequestDto requestDto) {
        if (RecordType.ACTIVE_USER_LOGIN.name().equals(requestDto.getRecordType())) {
            String userId = requestDto.getDimensions().get("userId");
            if (userId != null) {
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String currentMonthKey = "mau:" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                redisTemplate.opsForHyperLogLog().add(todayKey, userId);
                redisTemplate.opsForHyperLogLog().add(currentMonthKey, userId);
            }
        } else {
            AnalyticsRecord record = new AnalyticsRecord();
            record.setRecordType(RecordType.valueOf(requestDto.getRecordType()));
            record.setMetricName(requestDto.getMetricName());
            record.setMetricValue(requestDto.getMetricValue());
            record.setRecordTime(requestDto.getRecordTime());
            if (requestDto.getDimensions() != null) {
                requestDto.getDimensions().forEach((key, value) -> {
                    record.setDimensionKey(key);
                    record.setDimensionValue(value);
                    // This is a simplified approach; a real implementation might need a more
                    // complex way to handle multiple dimensions
                    PrivacyUtil.anonymizeRecord(record);
                    analyticsRepository.save(record);
                });
            } else {
                analyticsRepository.save(record);
            }
        }
    }

    @Override
    public AnalyticsResponseDto queryAnalytics(AnalyticsQueryDto queryDto) {
        return null;
    }

    @Override
    public ReportExportDto generateReport(ReportRequestDto reportRequestDto) {
        return null;
    }

    @Override
    public long getDailyActiveUsers(String dateStr) { // date in yyyy-MM-dd
        String dauKey = "dau:" + dateStr;
        Long size = redisTemplate.opsForHyperLogLog().size(dauKey);
        return size != null ? size : 0L;
    }

    @Override
    public long getMonthlyActiveUsers(String yearMonthStr) { // yearMonth in yyyy-MM
        String mauKey = "mau:" + yearMonthStr;
        Long size = redisTemplate.opsForHyperLogLog().size(mauKey);
        return size != null ? size : 0L;
    }

    @Override
    public double getAverageOrderValue(String startDateStr, String endDateStr) {
        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).plusDays(1).atStartOfDay(); // Exclusive end

        List<AnalyticsRecord> revenueRecords = analyticsRepository.findByRecordTypeAndRecordTimeBetween(
                RecordType.ORDER_REVENUE, startDate, endDate);
        List<AnalyticsRecord> completedOrderRecords = analyticsRepository.findByRecordTypeAndRecordTimeBetween(
                RecordType.COMPLETED_ORDERS_COUNT, startDate, endDate);

        double totalRevenue = revenueRecords.stream().mapToDouble(AnalyticsRecord::getMetricValue).sum();
        long totalCompletedOrders = completedOrderRecords.stream().mapToLong(r -> r.getMetricValue().longValue()).sum();

        if (totalCompletedOrders == 0)
            return 0.0;
        return totalRevenue / totalCompletedOrders;
    }

    @Override
    public double getDriverAcceptanceRate(String startDateStr, String endDateStr) {
        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).plusDays(1).atStartOfDay();

        long totalOrderRequests = analyticsRepository.countByRecordTypeAndRecordTimeBetween(
                RecordType.ORDER_REQUEST, startDate, endDate); // Or a more specific "orders_offered_to_drivers"
        long totalOrdersAccepted = analyticsRepository.countByRecordTypeAndRecordTimeBetween(
                RecordType.ORDER_ACCEPTED_BY_DRIVER, startDate, endDate);

        if (totalOrderRequests == 0)
            return 0.0;
        return ((double) totalOrdersAccepted / totalOrderRequests) * 100.0;
    }

    @Override
    public double getUserRetentionRate(String cohortMonthStr, int periodInMonths) { // cohortMonth e.g., "2023-01"
        YearMonth cohortMonth = YearMonth.parse(cohortMonthStr);
        LocalDateTime cohortStartDate = cohortMonth.atDay(1).atStartOfDay();
        LocalDateTime cohortEndDate = cohortMonth.atEndOfMonth().plusDays(1).atStartOfDay();

        // 1. Get users who registered in the cohort month
        List<AnalyticsRecord> cohortRegistrations = analyticsRepository.findByRecordTypeAndRecordTimeBetween(
                RecordType.USER_REGISTRATION, cohortStartDate, cohortEndDate);
        var cohortUserIds = cohortRegistrations.stream()
                .filter(ar -> "userId".equals(ar.getDimensionKey()))
                .map(AnalyticsRecord::getDimensionValue)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (cohortUserIds.isEmpty())
            return 0.0;

        // 2. Calculate retention using HyperLogLog
        String cohortHllKey = "retention_cohort:" + cohortMonthStr;
        Object[] cohortUserIdsArray = cohortUserIds.toArray();
        redisTemplate.opsForHyperLogLog().add(cohortHllKey, cohortUserIdsArray);
        long cohortSize = redisTemplate.opsForHyperLogLog().size(cohortHllKey);

        if (cohortSize == 0) {
            redisTemplate.delete(cohortHllKey);
            return 0.0;
        }

        YearMonth retentionCheckMonth = cohortMonth.plusMonths(periodInMonths);
        String retentionMauKey = "mau:" + retentionCheckMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        long retentionMauSize = redisTemplate.opsForHyperLogLog().size(retentionMauKey);

        String unionHllKey = "retention_union:" + cohortMonthStr;
        redisTemplate.opsForHyperLogLog().union(unionHllKey, cohortHllKey, retentionMauKey);
        long unionSize = redisTemplate.opsForHyperLogLog().size(unionHllKey);

        long intersectionSize = cohortSize + retentionMauSize - unionSize;

        // Clean up temporary HLL keys
        redisTemplate.delete(cohortHllKey);
        redisTemplate.delete(unionHllKey);

        return ((double) intersectionSize / cohortSize) * 100.0;
    }

    @Override
    public void calculateAndStoreDailyMetrics() {
        log.info("Calculating and storing daily summary metrics...");
    }

    @Override
    public void calculateAndStoreMonthlyMetrics() {
        log.info("Calculating and storing monthly summary metrics...");
    }

    @Override
    public DashboardSummaryDto getAdminDashboardSummary(String dateRangePreset) { // e.g., "TODAY", "LAST_7_DAYS"
        LocalDateTime startDate, endDate;
        LocalDate today = LocalDate.now();
        switch (dateRangePreset) {
            case "TODAY":
                startDate = today.atStartOfDay();
                endDate = today.plusDays(1).atStartOfDay();
                break;
            case "LAST_7_DAYS":
                startDate = today.minusDays(7).atStartOfDay();
                endDate = today.plusDays(1).atStartOfDay();
                break;
            default:
                // Default to today
                startDate = today.atStartOfDay();
                endDate = today.plusDays(1).atStartOfDay();
                break;
        }

        long totalOrders = analyticsRepository.countByRecordTypeAndRecordTimeBetween(RecordType.COMPLETED_ORDERS_COUNT,
                startDate, endDate);
        double totalRevenue = analyticsRepository
                .findByRecordTypeAndRecordTimeBetween(RecordType.ORDER_REVENUE, startDate, endDate)
                .stream().mapToDouble(AnalyticsRecord::getMetricValue).sum();
        long newUsers = analyticsRepository.countByRecordTypeAndRecordTimeBetween(RecordType.USER_REGISTRATION,
                startDate, endDate);
        long activeDrivers = getActiveDriversInRange(startDate, endDate); // Needs specific logic

        return new DashboardSummaryDto(totalOrders, totalRevenue, newUsers, activeDrivers);
    }

    private long getActiveDriversInRange(LocalDateTime startDate, LocalDateTime endDate) {
        return 0L;
    }

    @Override
    public List<TimeSeriesDataPointDto> getOrdersTrend(String startDateStr, String endDateStr, String granularity) {
        return List.of();
    }
}