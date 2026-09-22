package com.envmonitor.service;

import com.envmonitor.common.ClockCN;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.entity.PresenceEvent;
import com.envmonitor.repository.PresenceEventRepository;
import com.envmonitor.repository.SensorReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 红外进出事件：根据 PIR 状态跳变生成 ENTER/EXIT
 * 约定：pir=0 有人（进入），pir=1 无人（离开）
 */
@Service
public class PresenceEventService {

    private final PresenceEventRepository repository;
    private final EnvMonitorProperties properties;
    /** 每设备最近一次 PIR，用于检测跳变 */
    private final Map<String, Integer> lastPir = new ConcurrentHashMap<>();

    public PresenceEventService(PresenceEventRepository repository,
                                EnvMonitorProperties properties,
                                SensorReadingRepository sensorReadingRepository) {
        this.repository = repository;
        this.properties = properties;
        // 启动时用库中最新一帧初始化，避免重复 ENTER
        sensorReadingRepository.findTop50ByDeviceIdOrderByRecordedAtDesc(properties.getDefaultDeviceId())
                .stream()
                .findFirst()
                .ifPresent(r -> lastPir.put(normalize(r.getDeviceId()), r.getPir()));
    }

    @Transactional
    public PresenceEvent onSample(String deviceId, Integer pir,
                                  Double temp, Double humidity, Double mq135) {
        if (pir == null) {
            return null;
        }
        String id = normalize(deviceId);
        Integer prev = lastPir.put(id, pir);
        if (prev != null && prev.equals(pir)) {
            return null; // 状态未变
        }
        // 首次记录且为有人 → 也记一条，便于日志完整
        String type = (pir == 0) ? PresenceEvent.ENTER : PresenceEvent.EXIT;
        if (prev == null && pir != 0) {
            // 启动时就是无人，不记 EXIT
            return null;
        }

        PresenceEvent e = new PresenceEvent();
        e.setDeviceId(id);
        e.setEventType(type);
        e.setLabel(type.equals(PresenceEvent.ENTER) ? "有人进入房间" : "离开房间");
        e.setTemperature(temp);
        e.setHumidity(humidity);
        e.setMq135(mq135);
        e.setEventTime(ClockCN.now());
        return repository.save(e);
    }

    public List<PresenceEvent> recent(String deviceId) {
        return repository.findTop100ByDeviceIdOrderByEventTimeDesc(normalize(deviceId));
    }

    public List<PresenceEvent> inRange(String deviceId, LocalDateTime start) {
        return repository.findTop200ByDeviceIdAndEventTimeAfterOrderByEventTimeAsc(
                normalize(deviceId), start);
    }

    private String normalize(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return properties.getDefaultDeviceId();
        }
        return deviceId;
    }
}
