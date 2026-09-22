package com.envmonitor.service;

import com.envmonitor.config.EnvMonitorProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 设备侧 LED 开关状态（后端内存态，硬件轮询） */
@Service
public class DeviceControlService {

    private final EnvMonitorProperties properties;
    private final Map<String, Boolean> ledOn = new ConcurrentHashMap<>();

    public DeviceControlService(EnvMonitorProperties properties) {
        this.properties = properties;
        ledOn.put(properties.getDefaultDeviceId(), false);
    }

    public boolean setLed(String deviceId, boolean on) {
        String id = normalize(deviceId);
        ledOn.put(id, on);
        return on;
    }

    public boolean getLed(String deviceId) {
        return Boolean.TRUE.equals(ledOn.get(normalize(deviceId)));
    }

    private String normalize(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return properties.getDefaultDeviceId();
        }
        return deviceId;
    }
}
