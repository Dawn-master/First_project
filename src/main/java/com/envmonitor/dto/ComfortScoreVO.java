package com.envmonitor.dto;

import java.util.List;

public record ComfortScoreVO(
        int score,
        String level,
        String suggestion,
        List<String> factors
) {
}
