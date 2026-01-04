package com.easyride.analytics_service.controller;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.service.AnalyticsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/record")
    public ApiResponse<String> recordAnalyticsData(@Valid @RequestBody AnalyticsRequestDto requestDto) {
        log.info("Received request to record analytics data: {}", requestDto.getMetricName());
        analyticsService.recordAnalyticsData(requestDto);
        return ApiResponse.successMessage("分析数据记录成功");
    }

    @PostMapping("/query")
    public ApiResponse<AnalyticsResponseDto> queryAnalytics(@Valid @RequestBody AnalyticsQueryDto queryDto) {
        log.info("Received analytics query request: {}", queryDto);
        AnalyticsResponseDto responseDto = analyticsService.queryAnalytics(queryDto);
        return ApiResponse.success(responseDto);
    }

    @PostMapping("/report")
    public ApiResponse<ReportExportDto> generateReport(@Valid @RequestBody ReportRequestDto reportRequestDto) {
        log.info("Received report generation request: {}", reportRequestDto);
        ReportExportDto reportDto = analyticsService.generateReport(reportRequestDto);
        return ApiResponse.success(reportDto);
    }

    @GetMapping("/metrics/dau")
    public ApiResponse<Long> getDailyActiveUsers(@RequestParam String date) { // date in yyyy-MM-dd
        log.info("Requesting DAU for date: {}", date);
        long dau = analyticsService.getDailyActiveUsers(date);
        return ApiResponse.success(dau);
    }

    @GetMapping("/metrics/mau")
    public ApiResponse<Long> getMonthlyActiveUsers(@RequestParam String yearMonth) { // yearMonth in yyyy-MM
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
}