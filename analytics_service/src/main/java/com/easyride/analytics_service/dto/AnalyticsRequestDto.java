package com.easyride.analytics_service.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 前端请求要查询或统计的数据范围，例如时间段、维度、指标等
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsRequestDto {

    private String metricName;     // 要查询的指标，例如 "daily_orders"
    private String recordType;     // USER_DATA, ORDER_DATA, DRIVER_DATA
    private String dimensionKey;   // 按区域、车型等维度
    private String dimensionValue; // 具体的维度值
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
