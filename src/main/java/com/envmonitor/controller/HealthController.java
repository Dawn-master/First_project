package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.common.ClockCN;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/api/health"})
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "env-monitor-backend",
                "status", "UP",
                "time", ClockCN.now().toString()
        ));
    }
}
