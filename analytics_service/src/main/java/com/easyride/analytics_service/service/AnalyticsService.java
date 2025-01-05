package com.easyride.analytics_service.service;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.model.AnalyticsRecord;

public interface AnalyticsService {

    // 用于写入新数据记录，比如从其他微服务或MQ接收到相关运营数据
    void recordAnalyticsData(AnalyticsRecord record);

    // 用于前端或后台系统查询统计结果
    AnalyticsResponseDto queryAnalytics(AnalyticsRequestDto requestDto);

    // 生成报表并导出
    ReportExportDto generateReport(AnalyticsRequestDto requestDto);


}
