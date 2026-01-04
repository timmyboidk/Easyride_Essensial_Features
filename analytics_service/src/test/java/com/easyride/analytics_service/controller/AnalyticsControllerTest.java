package com.easyride.analytics_service.controller;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void recordAnalyticsData_Success() {
        AnalyticsRequestDto request = new AnalyticsRequestDto();
        request.setMetricName("test_metric");

        doNothing().when(analyticsService).recordAnalyticsData(any(AnalyticsRequestDto.class));

        ApiResponse<String> response = analyticsController.recordAnalyticsData(request);
        assertEquals(0, response.getCode());
    }

    @Test
    void queryAnalytics_Success() {
        AnalyticsQueryDto query = new AnalyticsQueryDto();
        AnalyticsResponseDto responseData = new AnalyticsResponseDto();
        when(analyticsService.queryAnalytics(any(AnalyticsQueryDto.class))).thenReturn(responseData);

        ApiResponse<AnalyticsResponseDto> response = analyticsController.queryAnalytics(query);
        assertEquals(0, response.getCode());
    }

    @Test
    void generateReport_Success() {
        ReportRequestDto request = new ReportRequestDto();
        ReportExportDto report = new ReportExportDto();
        when(analyticsService.generateReport(any(ReportRequestDto.class))).thenReturn(report);

        ApiResponse<ReportExportDto> response = analyticsController.generateReport(request);
        assertEquals(0, response.getCode());
    }

    @Test
    void getDailyActiveUsers_Success() {
        when(analyticsService.getDailyActiveUsers("2023-10-01")).thenReturn(100L);
        ApiResponse<Long> response = analyticsController.getDailyActiveUsers("2023-10-01");
        assertEquals(100L, response.getData());
    }
}
