-- Schema for analytics_records table
-- Drop table if exists to ensure clean state for tests
DROP TABLE IF EXISTS analytics_records;

CREATE TABLE analytics_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100),
    metric_value DOUBLE,
    record_time DATETIME,
    dimension_key VARCHAR(100),
    dimension_value VARCHAR(255),
    INDEX idx_record_type (record_type),
    INDEX idx_record_time (record_time),
    INDEX idx_record_type_time (record_type, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
