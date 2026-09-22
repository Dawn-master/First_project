package com.envmonitor.service;

import com.envmonitor.entity.AppUser;
import com.envmonitor.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    /** token -> username（内存会话，重启后需重新登录） */
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public AuthService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser register(String username, String password, String nickname) {
        String u = username == null ? "" : username.trim();
        String p = password == null ? "" : password;
        if (u.length() < 3 || u.length() > 32) {
            throw new IllegalArgumentException("用户名长度需 3-32 位");
        }
        if (p.length() < 6 || p.length() > 32) {
            throw new IllegalArgumentException("密码长度需 6-32 位");
        }
        if (!u.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("用户名仅支持字母、数字、下划线");
        }
        if (userRepository.existsByUsername(u)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        AppUser user = new AppUser();
        user.setUsername(u);
        user.setPasswordHash(sha256(p));
        user.setNickname(nickname == null || nickname.isBlank() ? u : nickname.trim());
        return userRepository.save(user);
    }

    public Map<String, Object> login(String username, String password) {
        String u = username == null ? "" : username.trim();
        AppUser user = userRepository.findByUsername(u)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在，请先注册"));
        if (!user.getPasswordHash().equals(sha256(password == null ? "" : password))) {
            throw new IllegalArgumentException("密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, u);
        return Map.of(
                "token", token,
                "username", user.getUsername(),
                "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname()
        );
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    public String usernameOf(String token) {
        return token == null ? null : sessions.get(token);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("hash fail", e);
        }
    }
}
