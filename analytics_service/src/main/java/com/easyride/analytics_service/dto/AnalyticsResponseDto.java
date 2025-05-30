package com.easyride.analytics_service.dto;

import lombok.*;

import java.util.List;

/**
 * 返回给前端的数据结果，可包含图表所需数据或统计值。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponseDto {

    private String metricName;
    private String recordType;
    private String dimensionKey;
    private String dimensionValue;

    // 返回一个明细或聚合后的数值
    private Double totalValue;  // 总和、平均值或其他统计结果
    private List<ChartDataDto> chartData; // 用于可视化图表的时间序列等
}
