package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.entity.AlertRecord;
import com.envmonitor.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<List<AlertRecord>> list(
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "false") boolean unresolvedOnly) {
        if (unresolvedOnly) {
            return ApiResponse.ok(alertService.listUnresolved(deviceId));
        }
        String id = (deviceId == null || deviceId.isBlank()) ? "sensor-001" : deviceId;
        return ApiResponse.ok(alertService.listRecent(id));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long id) {
        alertService.resolve(id);
        return ApiResponse.ok();
    }
}
