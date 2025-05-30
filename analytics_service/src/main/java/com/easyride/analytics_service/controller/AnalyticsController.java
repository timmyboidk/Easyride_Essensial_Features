package com.easyride.analytics_service.controller;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 接收来自其他微服务或后台的写入请求，用于存储新的分析数据。
     */
    @PostMapping("/record")
    public void recordData(@RequestBody AnalyticsRecord record) {
        // 这里可以进行简单校验或转换
        record.setRecordTime(LocalDateTime.now());
        analyticsService.recordAnalyticsData(record);
    }

    /**
     * 前端或管理后台调用，用于查询统计结果
     */
    @PostMapping("/query")
    public AnalyticsResponseDto queryAnalytics(@RequestBody AnalyticsRequestDto requestDto) {
        return analyticsService.queryAnalytics(requestDto);
    }

    /**
     * 导出报表示例
     */
    @PostMapping("/report")
    public ReportExportDto exportReport(@RequestBody AnalyticsRequestDto requestDto) {
        return analyticsService.generateReport(requestDto);
    }
}
