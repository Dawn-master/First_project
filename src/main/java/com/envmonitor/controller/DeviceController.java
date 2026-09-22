package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.DeviceDataRequest;
import com.envmonitor.dto.SensorLatestVO;
import com.envmonitor.service.SensorDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final SensorDataService sensorDataService;
    private final EnvMonitorProperties properties;

    public DeviceController(SensorDataService sensorDataService, EnvMonitorProperties properties) {
        this.sensorDataService = sensorDataService;
        this.properties = properties;
    }

    @PostMapping("/data")
    public ApiResponse<SensorLatestVO> upload(
            @RequestHeader(value = "X-Device-Key", required = false) String deviceKey,
            @Valid @RequestBody DeviceDataRequest request) {
        if (!isAuthorized(deviceKey)) {
            return ApiResponse.fail(401, "设备鉴权失败，请检查 X-Device-Key");
        }
        return ApiResponse.ok(sensorDataService.ingest(request));
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of(
                "status", "up",
                "defaultDeviceId", properties.getDefaultDeviceId(),
                "apiKeyHint", "X-Device-Key",
                "simulateEnabled", properties.isSimulateEnabled()
        ));
    }

    private boolean isAuthorized(String deviceKey) {
        String expected = properties.getDeviceApiKey();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return deviceKey == null || deviceKey.isBlank() || expected.equals(deviceKey);
    }
}
