package com.easyride.analytics_service.dto;

import lombok.*;
import java.util.List;

/**
 * 导出报表时使用的 DTO，例如导出PDF或Excel
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExportDto {
    private String reportTitle;
    private List<ChartDataDto> chartData;
    // 其他报表相关元数据
}
