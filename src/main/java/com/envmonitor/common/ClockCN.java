package com.envmonitor.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** 统一按北京时间（UTC+8）入库与返回，Navicat 里与电脑时钟一致 */
public final class ClockCN {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private ClockCN() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDateTime fromEpochMilli(Long epochMilli) {
        if (epochMilli == null) {
            return now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
    }
}
