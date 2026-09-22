package com.envmonitor.dto;

import java.time.LocalDateTime;

public record SensorLatestVO(
        String deviceId,
        Double temperature,
        Double humidity,
        Integer pir,
        Boolean presence,
        Double mq135,
        Double comfortScore,
        String airLevel,
        Boolean alerted,
        LocalDateTime recordedAt,
        Boolean led
) {
}
