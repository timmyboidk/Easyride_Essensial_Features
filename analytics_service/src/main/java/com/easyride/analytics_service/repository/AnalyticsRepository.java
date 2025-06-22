package com.easyride.analytics_service.repository;

import com.easyride.analytics_service.model.AnalyticsRecord;
import com.easyride.analytics_service.model.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<AnalyticsRecord, Long> {

    // 按类型、指标名、时间范围查询
    List<AnalyticsRecord> findByRecordTypeAndMetricNameAndRecordTimeBetween(
            RecordType recordType,
            String metricName,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // 按维度进行扩展查询
    List<AnalyticsRecord> findByRecordTypeAndDimensionKeyAndDimensionValue(
            RecordType recordType,
            String dimensionKey,
            String dimensionValue
    );

    List<AnalyticsRecord> findByRecordTypeAndRecordTimeBetween(
            RecordType recordType,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    long countByRecordTypeAndRecordTimeBetween(
            RecordType recordType,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}