package com.easyride.analytics_service.dto;

import lombok.*;

/**
 * 用于可视化图表的单个点，包含时间和数值
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataDto {

    private String label; // 时间或维度标签
    private Double value; // 对应指标数值
}
