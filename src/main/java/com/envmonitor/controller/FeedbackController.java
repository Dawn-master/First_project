package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.common.ClockCN;
import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.FeedbackRequest;
import com.envmonitor.entity.Feedback;
import com.envmonitor.repository.FeedbackRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackRepository repository;
    private final EnvMonitorProperties properties;

    public FeedbackController(FeedbackRepository repository, EnvMonitorProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** 提交报修/反馈 */
    @PostMapping
    public ApiResponse<Map<String, Object>> submit(@Valid @RequestBody FeedbackRequest req) {
        Feedback f = new Feedback();
        String deviceId = (req.getDeviceId() == null || req.getDeviceId().isBlank())
                ? properties.getDefaultDeviceId() : req.getDeviceId();
        f.setDeviceId(deviceId);
        String type = req.getType() == null || req.getType().isBlank() ? "REPAIR" : req.getType().toUpperCase();
        if (!type.equals("REPAIR") && !type.equals("SUGGEST") && !type.equals("OTHER")) {
            type = "REPAIR";
        }
        f.setType(type);
        f.setContact(req.getContact() == null ? "" : req.getContact());
        f.setContent(req.getContent().trim());
        f.setStatus("OPEN");
        f.setCreatedAt(ClockCN.now());
        repository.save(f);
        return ApiResponse.ok(Map.of(
                "id", f.getId(),
                "status", f.getStatus(),
                "createdAt", f.getCreatedAt().toString()
        ));
    }

    @GetMapping
    public ApiResponse<List<Feedback>> list(@RequestParam(required = false) String deviceId) {
        String id = (deviceId == null || deviceId.isBlank()) ? properties.getDefaultDeviceId() : deviceId;
        return ApiResponse.ok(repository.findTop50ByDeviceIdOrderByCreatedAtDesc(id));
    }
}
