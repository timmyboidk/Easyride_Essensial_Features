package com.easyride.analytics_service.controller;

import com.easyride.analytics_service.dto.*; // Ensure ApiResponse is here
import com.easyride.analytics_service.service.AnalyticsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List; // For potential list responses
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Existing endpoint, assuming AnalyticsRequestDto is for recording raw data points
    @PostMapping("/record")
    public ApiResponse<String> recordAnalyticsData(@Valid @RequestBody AnalyticsRequestDto requestDto) {
        log.info("Received request to record analytics data: {}", requestDto.getMetricName());
        analyticsService.recordAnalyticsData(requestDto); // Assuming this method still makes sense
        return ApiResponse.successMessage("分析数据记录成功");
    }

    // Existing endpoint, now returning ApiResponse
    @PostMapping("/query")
    public ApiResponse<AnalyticsResponseDto> queryAnalytics(@Valid @RequestBody AnalyticsQueryDto queryDto) { // New DTO for querying
        log.info("Received analytics query request: {}", queryDto);
        AnalyticsResponseDto responseDto = analyticsService.queryAnalytics(queryDto);
        return ApiResponse.success(responseDto);
    }

    // Existing endpoint, now returning ApiResponse
    @PostMapping("/report")
    public ApiResponse<ReportExportDto> generateReport(@Valid @RequestBody ReportRequestDto reportRequestDto) { // New DTO for report requests
        log.info("Received report generation request: {}", reportRequestDto);
        ReportExportDto reportDto = analyticsService.generateReport(reportRequestDto);
        return ApiResponse.success(reportDto);
    }

    // New endpoint example for specific metrics
    @GetMapping("/metrics/dau")
    public ApiResponse<Long> getDailyActiveUsers(@RequestParam String date) { // date in YYYY-MM-DD
        log.info("Requesting DAU for date: {}", date);
        long dau = analyticsService.getDailyActiveUsers(date);
        return ApiResponse.success(dau);
    }

    @GetMapping("/metrics/mau")
    public ApiResponse<Long> getMonthlyActiveUsers(@RequestParam String yearMonth) { // yearMonth in YYYY-MM
        log.info("Requesting MAU for month: {}", yearMonth);
        long mau = analyticsService.getMonthlyActiveUsers(yearMonth);
        return ApiResponse.success(mau);
    }

    @GetMapping("/metrics/average-order-value")
    public ApiResponse<Double> getAverageOrderValue(@RequestParam String startDate, @RequestParam String endDate) {
        log.info("Requesting Average Order Value (AOV) between {} and {}", startDate, endDate);
        double aov = analyticsService.getAverageOrderValue(startDate, endDate);
        return ApiResponse.success(aov);
    }

    // More endpoints for other specific metrics (driver acceptance, retention, etc.)
}