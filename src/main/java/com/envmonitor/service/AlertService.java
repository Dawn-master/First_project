package com.envmonitor.service;

import com.envmonitor.common.ClockCN;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.entity.AlertRecord;
import com.envmonitor.repository.AlertRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final AlertRecordRepository alertRecordRepository;
    private final EnvMonitorProperties properties;

    public AlertService(AlertRecordRepository alertRecordRepository, EnvMonitorProperties properties) {
        this.alertRecordRepository = alertRecordRepository;
        this.properties = properties;
    }

    @Transactional
    public List<AlertRecord> checkAndSave(String deviceId,
                                          Double temperature,
                                          Double humidity,
                                          Double mq135) {
        EnvMonitorProperties.Thresholds t = properties.getThresholds();
        List<AlertRecord> records = new ArrayList<>();

        // 与硬件一致：仅“超过”阈值报警
        if (temperature != null && temperature > t.getTempMax()) {
            records.add(build(deviceId, "TEMP", "WARN",
                    String.format("温度 %.1f℃ 超过阈值 %.0f℃", temperature, t.getTempMax()),
                    temperature));
        }

        if (humidity != null && humidity > t.getHumidityMax()) {
            records.add(build(deviceId, "HUMIDITY", "WARN",
                    String.format("湿度 %.1f%% 超过阈值 %.0f%%", humidity, t.getHumidityMax()),
                    humidity));
        }

        if (mq135 != null && mq135 > t.getMq135Warn()) {
            records.add(build(deviceId, "MQ135", "WARN",
                    String.format("MQ135 %.1f 超过阈值 %.0f", mq135, t.getMq135Warn()),
                    mq135));
        }

        if (records.isEmpty()) {
            return List.of();
        }
        return alertRecordRepository.saveAll(records);
    }

    private AlertRecord build(String deviceId, String type, String level, String message, Double value) {
        AlertRecord record = new AlertRecord();
        record.setDeviceId(deviceId);
        record.setAlertType(type);
        record.setLevel(level);
        record.setMessage(message);
        record.setTriggerValue(value);
        record.setCreatedAt(ClockCN.now());
        return record;
    }

    public List<AlertRecord> listRecent(String deviceId) {
        return alertRecordRepository.findTop50ByDeviceIdOrderByCreatedAtDesc(deviceId);
    }

    public List<AlertRecord> listUnresolved(String deviceId) {
        String id = (deviceId == null || deviceId.isBlank()) ? "sensor-001" : deviceId;
        return alertRecordRepository.findByDeviceIdAndResolvedFalseOrderByCreatedAtDesc(id);
    }

    @Transactional
    public void resolve(Long id) {
        alertRecordRepository.findById(id).ifPresent(alert -> {
            alert.setResolved(true);
            alertRecordRepository.save(alert);
        });
    }
}
