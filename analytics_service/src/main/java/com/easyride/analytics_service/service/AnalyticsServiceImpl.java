package com.easyride.analytics_service.service;

import com.easyride.analytics_service.dto.*;
import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import com.easyride.analytics_service.repository.AnalyticsRepository;
import com.easyride.analytics_service.util.PrivacyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional
    @Override
    public void recordAnalyticsData(AnalyticsRecord record) {
        // 记录入库前，如需匿名化或脱敏可在此处理
        PrivacyUtil.anonymizeRecord(record);
        analyticsRepository.save(record);
    }

    @Override
    public AnalyticsResponseDto queryAnalytics(AnalyticsRequestDto requestDto) {
        // 将字符串转为枚举
        RecordType recordType = RecordType.valueOf(requestDto.getRecordType().toUpperCase());
        List<AnalyticsRecord> records = analyticsRepository.findByRecordTypeAndMetricNameAndRecordTimeBetween(
            recordType,
            requestDto.getMetricName(),
            requestDto.getStartTime(),
            requestDto.getEndTime()
        );

        // 进行聚合统计，如求和
        double totalValue = records.stream()
            .mapToDouble(AnalyticsRecord::getMetricValue)
            .sum();

        // 生成可视化图表数据，如按天/小时聚合
        // 这里只做最简单的把每条记录返回
        List<ChartDataDto> chartData = records.stream()
            .map(r -> ChartDataDto.builder()
                .label(r.getRecordTime().toString())
                .value(r.getMetricValue())
                .build()
            ).collect(Collectors.toList());

        return AnalyticsResponseDto.builder()
            .metricName(requestDto.getMetricName())
            .recordType(requestDto.getRecordType())
            .dimensionKey(requestDto.getDimensionKey())
            .dimensionValue(requestDto.getDimensionValue())
            .totalValue(totalValue)
            .chartData(chartData)
            .build();
    }

    @Override
    public ReportExportDto generateReport(AnalyticsRequestDto requestDto) {
        // 先获取数据
        AnalyticsResponseDto response = queryAnalytics(requestDto);

        // 这里示例返回一个报表 DTO，可在 Controller 层选择导出 PDF/Excel
        return ReportExportDto.builder()
            .reportTitle("数据报表：" + requestDto.getMetricName())
            .chartData(response.getChartData())
            .build();
    }
}
