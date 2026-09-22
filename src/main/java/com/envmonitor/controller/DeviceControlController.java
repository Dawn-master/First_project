package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.service.DeviceControlService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LED 远程开关 + 硬件拉取指令。
 * 前端：POST /api/device/led
 * 硬件：GET  /api/device/led?deviceId=...  或读上报接口返回中的 led 字段
 */
@RestController
@RequestMapping("/api/device")
public class DeviceControlController {

    private final DeviceControlService controlService;
    private final EnvMonitorProperties properties;

    public DeviceControlController(DeviceControlService controlService, EnvMonitorProperties properties) {
        this.controlService = controlService;
        this.properties = properties;
    }

    public static class LedRequest {
        public String deviceId;
        /** true 开灯 / false 关灯 */
        public Boolean on;
    }

    @PostMapping("/led")
    public ApiResponse<Map<String, Object>> setLed(@RequestBody LedRequest req) {
        if (req == null || req.on == null) {
            return ApiResponse.fail("on 必须为 true/false");
        }
        boolean on = controlService.setLed(req.deviceId, req.on);
        return ApiResponse.ok(Map.of(
                "deviceId", req.deviceId == null || req.deviceId.isBlank()
                        ? properties.getDefaultDeviceId() : req.deviceId,
                "led", on
        ));
    }

    @GetMapping("/led")
    public ApiResponse<Map<String, Object>> getLed(@RequestParam(required = false) String deviceId) {
        return ApiResponse.ok(Map.of(
                "deviceId", deviceId == null || deviceId.isBlank() ? properties.getDefaultDeviceId() : deviceId,
                "led", controlService.getLed(deviceId)
        ));
    }
}
