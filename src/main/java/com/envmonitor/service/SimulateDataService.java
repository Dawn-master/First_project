package com.envmonitor.service;

import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.DeviceDataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 已按项目要求禁用模拟数据：仅保留真实硬件上报。
 */
@Service
public class SimulateDataService {

    private static final Logger log = LoggerFactory.getLogger(SimulateDataService.class);

    private final EnvMonitorProperties properties;

    public SimulateDataService(EnvMonitorProperties properties) {
        this.properties = properties;
        if (properties.isSimulateEnabled()) {
            log.warn("simulate-enabled 配置为 true，但服务已硬禁用模拟数据");
        }
    }

    public boolean start() {
        log.info("模拟数据已禁用，请使用硬件上报");
        return false;
    }

    public boolean stop() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public DeviceDataRequest nextFrame() {
        throw new IllegalStateException("模拟数据已禁用");
    }
}
