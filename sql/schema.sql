-- MySQL 8 初始化：多传感器环境监测系统
CREATE DATABASE IF NOT EXISTS env_monitor
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE env_monitor;

CREATE TABLE IF NOT EXISTS sensor_reading (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(64) NOT NULL,
    temperature DOUBLE NOT NULL,
    humidity DOUBLE NOT NULL,
    pir INT NOT NULL,
    mq135 DOUBLE NOT NULL,
    comfort_score DOUBLE NULL,
    alerted TINYINT NOT NULL DEFAULT 0,
    recorded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sensor_device_time (device_id, recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    level VARCHAR(32) NOT NULL,
    message VARCHAR(255) NOT NULL,
    trigger_value DOUBLE NULL,
    resolved TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_alert_device_time (device_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NULL,
    api_key VARCHAR(128) NULL,
    last_seen_at DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO device (id, name, api_key)
VALUES ('sensor-001', 'monitor-node-1', 'env-monitor-2026');
