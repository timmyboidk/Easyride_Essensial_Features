package com.easyride.analytics_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 数据分析记录，用于存储我们收集到的运营数据。
 * 可以将其设计成明细表，或者在后续再做汇总表。
 */
@TableName("analytics_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 用于区分用户数据、订单数据、司机数据等类型
    private RecordType recordType;

    // 关键指标，如 “活跃用户数”、“日订单量” 等，可使用 json 或多列
    private String metricName;

    // 指标对应的数值
    private Double metricValue;

    // 数据发生时间或统计时间
    private LocalDateTime recordTime;

    // 其他可扩展字段，如区域、车型等维度
    private String dimensionKey;
    private String dimensionValue;
}
