package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.dto.ComfortScoreVO;
import com.envmonitor.dto.HistoryVO;
import com.envmonitor.dto.SensorLatestVO;
import com.envmonitor.service.SensorDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor")
public class SensorController {

    private final SensorDataService sensorDataService;

    public SensorController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping("/latest")
    public ApiResponse<SensorLatestVO> latest(@RequestParam(required = false) String deviceId) {
        return ApiResponse.ok(sensorDataService.latest(deviceId));
    }

    @GetMapping("/history")
    public ApiResponse<HistoryVO> history(
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.ok(sensorDataService.history(deviceId, hours));
    }

    @GetMapping("/score")
    public ApiResponse<ComfortScoreVO> score(@RequestParam(required = false) String deviceId) {
        return ApiResponse.ok(sensorDataService.score(deviceId));
    }

    @GetMapping("/recent")
    public ApiResponse<List<SensorLatestVO>> recent(@RequestParam(required = false) String deviceId) {
        return ApiResponse.ok(sensorDataService.recentList(deviceId));
    }

    /** 实时监测：最新硬件帧 + LED + 阈值报警（无数据时 hasData=false） */
    @GetMapping("/realtime")
    public ApiResponse<Map<String, Object>> realtime(@RequestParam(required = false) String deviceId) {
        return ApiResponse.ok(sensorDataService.realtime(deviceId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "24") int hours) {
        return ApiResponse.ok(Map.of(
                "realtime", sensorDataService.realtime(deviceId),
                "latest", sensorDataService.latest(deviceId),
                "score", sensorDataService.score(deviceId)
        ));
    }
}
