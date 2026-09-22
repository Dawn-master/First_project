package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 模拟数据接口已禁用：项目仅接受真实硬件上报 */
@RestController
@RequestMapping("/api/simulate")
public class SimulateController {

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start() {
        return ApiResponse.fail(403, "模拟数据已禁用，请使用硬件实时上报");
    }

    @PostMapping("/stop")
    public ApiResponse<Map<String, Object>> stop() {
        return ApiResponse.ok(Map.of("running", false));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of("running", false, "disabled", true));
    }

    @PostMapping("/once")
    public ApiResponse<Map<String, Object>> once() {
        return ApiResponse.fail(403, "模拟数据已禁用，请使用硬件实时上报");
    }
}
