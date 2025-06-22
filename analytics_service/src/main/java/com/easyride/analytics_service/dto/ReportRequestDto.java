package com.easyride.analytics_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportRequestDto {
    private String reportName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}