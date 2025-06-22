package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnalyticsQueryDto {
    private String metricName;
    private String recordType;
    private String dimensionKey;
    private String dimensionValue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}