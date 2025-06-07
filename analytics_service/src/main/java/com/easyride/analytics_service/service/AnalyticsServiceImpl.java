package com.easyride.analytics_service.service;

// ... imports ...
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import org.springframework.data.redis.core.RedisTemplate; // For HyperLogLog (DAU/MAU)
import org.springframework.scheduling.annotation.Scheduled; // For batch calcs
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    // ... existing fields (repository, privacyUtil) ...
    private final RedisTemplate<String, String> redisTemplate; // For unique user tracking with HLL or Sets

    @Autowired
    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository, PrivacyUtil privacyUtil, RedisTemplate<String, String> redisTemplate) {
        this.analyticsRepository = analyticsRepository;
        this.privacyUtil = privacyUtil;
        this.redisTemplate = redisTemplate;
    }

    // Existing recordAnalyticsData method
    @Override
    public void recordAnalyticsData(AnalyticsRequestDto requestDto) {
        // ... (existing logic) ...
        // For DAU/MAU: if event is a user login or significant action:
        if (RecordType.ACTIVE_USER_LOGIN.name().equals(requestDto.getRecordType())) {
            String userId = requestDto.getDimensions().get("userId");
            if (userId != null) {
                String todayKey = "dau:" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                String currentMonthKey = "mau:" + YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                redisTemplate.opsForHyperLogLog().add(todayKey, userId);
                redisTemplate.opsForHyperLogLog().add(currentMonthKey, userId);
                // Set TTL for these keys if needed, e.g., DAU keys expire after 2 days, MAU after ~40 days.
            }
        }
    }

    @Override
    public long getDailyActiveUsers(String dateStr) { // date in YYYY-MM-DD
        String dauKey = "dau:" + dateStr;
        Long size = redisTemplate.opsForHyperLogLog().size(dauKey);
        return size != null ? size : 0L;
    }

    @Override
    public long getMonthlyActiveUsers(String yearMonthStr) { // yearMonth in YYYY-MM
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

        if (totalCompletedOrders == 0) return 0.0;
        return totalRevenue / totalCompletedOrders;
    }

    @Override
    public double getDriverAcceptanceRate(String startDateStr, String endDateStr) {
        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).plusDays(1).atStartOfDay();

        // Needs: Count of orders offered to drivers (or all created orders eligible for matching)
        // And: Count of orders accepted by drivers
        // These metrics need to be recorded from OrderService or MatchingService events
        long totalOrderRequests = analyticsRepository.countByRecordTypeAndRecordTimeBetween(
                RecordType.ORDER_REQUEST, startDate, endDate); // Or a more specific "orders_offered_to_drivers"
        long totalOrdersAccepted = analyticsRepository.countByRecordTypeAndRecordTimeBetween(
                RecordType.ORDER_ACCEPTED_BY_DRIVER, startDate, endDate);

        if (totalOrderRequests == 0) return 0.0;
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
        Set<String> cohortUserIds = cohortRegistrations.stream()
                .map(ar -> ar.getDimensions().get("userId"))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (cohortUserIds.isEmpty()) return 0.0;

        // 2. Check how many of these cohort users were active in the target retention month
        YearMonth retentionCheckMonth = cohortMonth.plusMonths(periodInMonths);
        String retentionMauKey = "mau:" + retentionCheckMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        long retainedUsersCount = cohortUserIds.stream()
                .filter(userId -> {
                    Long isMember = redisTemplate.opsForHyperLogLog().count(retentionMauKey, userId); // HLL pfcount userIdKey -> 1 if present, 0 if not
                    return isMember != null && isMember > 0; // Check if user ID was added to that month's HLL
                })
                .count();

        return ((double) retainedUsersCount / cohortUserIds.size()) * 100.0;
    }


    // ... (implement generateReport, queryAnalytics as needed)

    // Placeholder for methods called by scheduler
    @Override
    // @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void calculateAndStoreDailyMetrics() {
        log.info("Calculating and storing daily summary metrics...");
        // Example: Calculate daily revenue, total orders from raw records and store as a single daily summary record
        // This helps in faster querying for dashboards rather than always aggregating raw data.
    }

    @Override
    // @Scheduled(cron = "0 0 2 1 * ?") // Run monthly on 1st day at 2 AM
    public void calculateAndStoreMonthlyMetrics() {
        log.info("Calculating and storing monthly summary metrics...");
        // Example: MAU (if not relying solely on HLL for final reporting), monthly retention, etc.
    }

    public DashboardSummaryDto getAdminDashboardSummary(String dateRangePreset) { // e.g., "TODAY", "LAST_7_DAYS"
        LocalDateTime startDate, endDate;
        // ... determine startDate, endDate from preset ...

        // Example metrics
        long totalOrders = analyticsRepository.countByRecordTypeAndRecordTimeBetween(RecordType.COMPLETED_ORDERS_COUNT, startDate, endDate);
        double totalRevenue = analyticsRepository.findByRecordTypeAndRecordTimeBetween(RecordType.ORDER_REVENUE, startDate, endDate)
                .stream().mapToDouble(AnalyticsRecord::getMetricValue).sum();
        long newUsers = analyticsRepository.countByRecordTypeAndRecordTimeBetween(RecordType.USER_REGISTRATION, startDate, endDate);
        long activeDrivers = getActiveDriversInRange(startDate, endDate); // Needs specific logic

        return new DashboardSummaryDto(totalOrders, totalRevenue, newUsers, activeDrivers);
    }

    private long getActiveDriversInRange(LocalDateTime startDate, LocalDateTime endDate) {
        // This would require tracking driver activity (e.g., logins, completed trips, online time)
        // One way: query unique driverIds from COMPLETED_ORDERS_COUNT or DRIVER_ONLINE_SESSION records in the range
        // Example:
        // List<AnalyticsRecord> driverActivity = analyticsRepository.findDistinctDriversByRecordTypesAndRecordTimeBetween(
        //     List.of(RecordType.COMPLETED_ORDERS_COUNT, RecordType.DRIVER_ONLINE_SESSION), // Define this query
        //     startDate, endDate
        // );
        // return driverActivity.stream().map(ar -> ar.getDimensions().get("driverId")).distinct().count();
        return 0L; // Placeholder
    }

    // Method to provide data for a time-series chart (e.g., orders per day)
    public List<TimeSeriesDataPointDto> getOrdersTrend(String startDateStr, String endDateStr, String granularity) { // granularity: "HOURLY", "DAILY"
        // Query AnalyticsRecord for COMPLETED_ORDERS_COUNT, group by hour/day, sum metricValue
        // ...
        return List.of(); // Placeholder
    }
}