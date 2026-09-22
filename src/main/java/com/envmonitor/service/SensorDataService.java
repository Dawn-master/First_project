package com.envmonitor.service;

import com.envmonitor.common.ClockCN;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.ComfortScoreVO;
import com.envmonitor.dto.DeviceDataRequest;
import com.envmonitor.dto.HistoryVO;
import com.envmonitor.dto.SensorLatestVO;
import com.envmonitor.entity.AlertRecord;
import com.envmonitor.entity.SensorReading;
import com.envmonitor.repository.SensorReadingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SensorDataService {

    private final SensorReadingRepository repository;
    private final ComfortScoreService comfortScoreService;
    private final AlertService alertService;
    private final EnvMonitorProperties properties;
    private final DeviceControlService deviceControlService;
    private final ThresholdService thresholdService;
    private final PresenceEventService presenceEventService;
    /** 记录上一次是否报警，用于“报警恢复后自动关灯” */
    private final Map<String, Boolean> lastAlerted = new ConcurrentHashMap<>();

    public SensorDataService(SensorReadingRepository repository,
                             ComfortScoreService comfortScoreService,
                             AlertService alertService,
                             EnvMonitorProperties properties,
                             DeviceControlService deviceControlService,
                             ThresholdService thresholdService,
                             PresenceEventService presenceEventService) {
        this.repository = repository;
        this.comfortScoreService = comfortScoreService;
        this.alertService = alertService;
        this.properties = properties;
        this.deviceControlService = deviceControlService;
        this.thresholdService = thresholdService;
        this.presenceEventService = presenceEventService;
    }

    @Transactional
    public SensorLatestVO ingest(DeviceDataRequest request) {
        ComfortScoreVO score = comfortScoreService.evaluate(
                request.getTemperature(), request.getHumidity(), request.getMq135());
        List<AlertRecord> alerts = alertService.checkAndSave(
                request.getDeviceId(),
                request.getTemperature(),
                request.getHumidity(),
                request.getMq135());

        SensorReading reading = new SensorReading();
        reading.setDeviceId(request.getDeviceId());
        reading.setTemperature(request.getTemperature());
        reading.setHumidity(request.getHumidity());
        reading.setPir(request.getPir());
        reading.setMq135(request.getMq135());
        reading.setComfortScore((double) score.score());
        reading.setAlerted(!alerts.isEmpty());
        reading.setRecordedAt(ClockCN.fromEpochMilli(request.getTimestampMs()));

        repository.save(reading);

        // 红外跳变 → 进出事件日志
        presenceEventService.onSample(
                request.getDeviceId(),
                request.getPir(),
                request.getTemperature(),
                request.getHumidity(),
                request.getMq135());

        // 报警结束（由超标变为正常）→ 后端自动把 LED 置为关
        // 这样硬件下次上报拿到 led:false，灯会自动熄灭；不影响正常时小程序远程开关
        String devId = (request.getDeviceId() == null || request.getDeviceId().isBlank())
                ? properties.getDefaultDeviceId() : request.getDeviceId();
        boolean nowAlert = !alerts.isEmpty();
        Boolean prevAlert = lastAlerted.put(devId, nowAlert);
        if (Boolean.TRUE.equals(prevAlert) && !nowAlert) {
            deviceControlService.setLed(devId, false);
        }

        return toLatestVO(reading, comfortScoreService.airLevel(reading.getMq135()));
    }

    public SensorLatestVO latest(String deviceId) {
        String id = normalizeDevice(deviceId);
        return repository.findFirstByDeviceIdOrderByRecordedAtDesc(id)
                .map(r -> toLatestVO(r, comfortScoreService.airLevel(r.getMq135())))
                .orElseThrow(() -> new NoSuchElementException("设备 " + id + " 暂无数据"));
    }

    public HistoryVO history(String deviceId, int hours) {
        String id = normalizeDevice(deviceId);
        int h = Math.max(1, Math.min(hours, 24 * 7));
        LocalDateTime start = ClockCN.now().minusHours(h);

        // 1) 区间全量统计（count/avg/max 都基于整段数据，而不是只统计曲线点）
        HistoryVO.Stats stats = loadStats(id, start);

        // 2) 曲线点：取区间内【最新的 500 条】再按时间正序绘制
        //    （不是最早 500 条，避免高峰被截在窗口外）
        List<SensorReading> recentDesc = repository
                .findByDeviceIdAndRecordedAtAfterOrderByRecordedAtDesc(id, start, PageRequest.of(0, 500));
        List<SensorReading> forChart = new java.util.ArrayList<>(recentDesc);
        java.util.Collections.reverse(forChart);

        return HistoryVO.of(id, h, stats, forChart);
    }

    private HistoryVO.Stats loadStats(String deviceId, LocalDateTime start) {
        try {
            long count = repository.countInRange(deviceId, start);
            if (count <= 0) {
                return new HistoryVO.Stats(0, null, null, null, null, null, null);
            }
            Double avgT = repository.avgTempInRange(deviceId, start);
            Double avgH = repository.avgHumInRange(deviceId, start);
            Double avgMq = repository.avgMqInRange(deviceId, start);
            Double maxMq = repository.maxMqInRange(deviceId, start);
            Double minMq = repository.minMqInRange(deviceId, start);
            long present = repository.countPresentInRange(deviceId, start);
            double rate = present * 1.0 / count;
            return new HistoryVO.Stats(count, avgT, avgH, avgMq, maxMq, minMq, rate);
        } catch (Exception ex) {
            return new HistoryVO.Stats(0, null, null, null, null, null, null);
        }
    }

    public ComfortScoreVO score(String deviceId) {
        SensorLatestVO latest = latest(deviceId);
        return comfortScoreService.evaluate(
                latest.temperature(), latest.humidity(), latest.mq135());
    }

    public List<SensorLatestVO> recentList(String deviceId) {
        String id = normalizeDevice(deviceId);
        return repository.findTop50ByDeviceIdOrderByRecordedAtDesc(id).stream()
                .map(r -> toLatestVO(r, comfortScoreService.airLevel(r.getMq135())))
                .toList();
    }

    /** 实时监测聚合：最新值 + LED 状态 + 阈值报警 */
    public Map<String, Object> realtime(String deviceId) {
        SensorLatestVO latest;
        try {
            latest = latest(deviceId);
        } catch (NoSuchElementException ex) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("hasData", false);
            empty.put("latest", null);
            empty.put("led", deviceControlService.getLed(deviceId));
            empty.put("threshold", thresholdService.evaluate(null));
            empty.put("score", null);
            return empty;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hasData", true);
        body.put("latest", latest);
        body.put("led", deviceControlService.getLed(latest.deviceId()));
        body.put("threshold", thresholdService.evaluate(latest));
        body.put("score", comfortScoreService.evaluate(
                latest.temperature(), latest.humidity(), latest.mq135()));
        return body;
    }

    private String normalizeDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return properties.getDefaultDeviceId();
        }
        return deviceId;
    }

    private SensorLatestVO toLatestVO(SensorReading r, String airLevel) {
        // 红外 PA0 低电平有效：0=有人，1=无人（与 HW 模块一致）
        Integer pir = r.getPir();
        boolean presence = pir != null && pir == 0;
        return new SensorLatestVO(
                r.getDeviceId(),
                r.getTemperature(),
                r.getHumidity(),
                pir,
                presence,
                r.getMq135(),
                r.getComfortScore(),
                airLevel,
                r.getAlerted(),
                r.getRecordedAt(),
                deviceControlService.getLed(r.getDeviceId())
        );
    }
}
