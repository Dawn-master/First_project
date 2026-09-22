package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.common.ClockCN;
import com.envmonitor.entity.PresenceEvent;
import com.envmonitor.service.PresenceEventService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 红外人员进出事件日志 */
@RestController
@RequestMapping("/api/device/events")
public class PresenceEventController {

    private final PresenceEventService presenceEventService;

    public PresenceEventController(PresenceEventService presenceEventService) {
        this.presenceEventService = presenceEventService;
    }

    /** 最近进出记录（新→旧） */
    @GetMapping
    public ApiResponse<List<PresenceEvent>> list(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) Integer hours) {
        if (hours != null && hours > 0) {
            LocalDateTime start = ClockCN.now().minusHours(hours);
            List<PresenceEvent> list = presenceEventService.inRange(deviceId, start);
            return ApiResponse.ok(list);
        }
        return ApiResponse.ok(presenceEventService.recent(deviceId));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam(required = false) String deviceId) {
        List<PresenceEvent> list = presenceEventService.recent(deviceId);
        long enter = list.stream().filter(e -> PresenceEvent.ENTER.equals(e.getEventType())).count();
        long exit = list.stream().filter(e -> PresenceEvent.EXIT.equals(e.getEventType())).count();
        return ApiResponse.ok(Map.of(
                "total", list.size(),
                "enterCount", enter,
                "exitCount", exit,
                "latest", list.isEmpty() ? null : list.get(0)
        ));
    }
}
