package com.envmonitor.controller;

import com.envmonitor.common.ApiResponse;
import com.envmonitor.common.ClockCN;
import com.envmonitor.entity.AppUser;
import com.envmonitor.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static class RegisterReq {
        @NotBlank
        @Size(min = 3, max = 32)
        public String username;
        @NotBlank
        @Size(min = 6, max = 32)
        public String password;
        public String nickname;
    }

    public static class LoginReq {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterReq req) {
        try {
            AppUser u = authService.register(req.username, req.password, req.nickname);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", u.getId());
            data.put("username", u.getUsername());
            data.put("nickname", u.getNickname());
            data.put("createdAt", ClockCN.now().toString());
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginReq req) {
        try {
            return ApiResponse.ok(authService.login(req.username, req.password));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.logout(token);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@RequestHeader(value = "X-Token", required = false) String token) {
        String username = authService.usernameOf(token);
        if (username == null) {
            return ApiResponse.fail(401, "未登录或登录已过期");
        }
        return ApiResponse.ok(Map.of("username", username));
    }
}
