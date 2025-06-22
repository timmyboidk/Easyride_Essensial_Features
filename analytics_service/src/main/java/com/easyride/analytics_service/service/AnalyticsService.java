package com.easyride.analytics_service.service;

import com.easyride.analytics_service.dto.*;

import java.util.List;

public interface AnalyticsService {
    void recordAnalyticsData(AnalyticsRequestDto requestDto);
    AnalyticsResponseDto queryAnalytics(AnalyticsQueryDto queryDto); // Keep if you have generic query
    ReportExportDto generateReport(ReportRequestDto reportRequestDto); // Keep if generic

    // New specific metric methods
    long getDailyActiveUsers(String date); // yyyy-MM-dd
    long getMonthlyActiveUsers(String yearMonth); // yyyy-MM
    double getAverageOrderValue(String startDate, String endDate); // yyyy-MM-dd
    double getDriverAcceptanceRate(String startDate, String endDate); // %
    double getUserRetentionRate(String cohortMonth, int periodInMonths); // e.g., cohort "2023-01", period 1 (for 1-month retention)
    // Add methods for driver retention, etc.

    void calculateAndStoreDailyMetrics(); // For scheduler
    void calculateAndStoreMonthlyMetrics(); // For scheduler
    DashboardSummaryDto getAdminDashboardSummary(String today);
    List<TimeSeriesDataPointDto> getOrdersTrend(String startDate, String endDate, String granularity);
}