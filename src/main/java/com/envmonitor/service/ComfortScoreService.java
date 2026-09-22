package com.envmonitor.service;

import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.ComfortScoreVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 综合评分（展示用）。报警判定以 ThresholdService / 硬件规则为准：
 * 温度&gt;30、湿度&gt;50、MQ135&gt;200。
 */
@Service
public class ComfortScoreService {

    private final EnvMonitorProperties properties;

    public ComfortScoreService(EnvMonitorProperties properties) {
        this.properties = properties;
    }

    public ComfortScoreVO evaluate(Double temperature, Double humidity, Double mq135) {
        EnvMonitorProperties.Thresholds t = properties.getThresholds();
        List<String> factors = new ArrayList<>();

        double tempScore = upperPenalty(temperature, t.getTempMax(), 0.15);
        double humidityScore = upperPenalty(humidity, t.getHumidityMax(), 0.2);
        double airScore = upperPenalty(mq135, t.getMq135Warn(), 0.02);

        if (temperature != null && temperature > t.getTempMax()) {
            factors.add(String.format("温度 %.1f℃ 超过 %.0f℃", temperature, t.getTempMax()));
        }
        if (humidity != null && humidity > t.getHumidityMax()) {
            factors.add(String.format("湿度 %.1f%% 超过 %.0f%%", humidity, t.getHumidityMax()));
        }
        if (mq135 != null && mq135 > t.getMq135Warn()) {
            factors.add(String.format("MQ135 %.1f 超过 %.0f", mq135, t.getMq135Warn()));
        }

        int score = (int) Math.round(tempScore * 0.3 + humidityScore * 0.25 + airScore * 0.45);
        score = Math.max(0, Math.min(100, score));

        String level = score >= 80 ? "优" : score >= 60 ? "良" : score >= 40 ? "中" : "差";
        String suggestion = factors.isEmpty()
                ? "环境正常，未超报警阈值"
                : String.join("；", factors) + "（与硬件报警条件一致）";

        return new ComfortScoreVO(score, level, suggestion, factors);
    }

    /** 未超上限得 100，超过则按超出比例扣分 */
    private double upperPenalty(Double value, double max, double penaltyPerUnit) {
        if (value == null) {
            return 0;
        }
        if (value <= max) {
            return 100;
        }
        double over = value - max;
        return Math.max(0, 100 - over * penaltyPerUnit * 100 / Math.max(max, 1) * 2);
    }

    public String airLevel(Double mq135) {
        EnvMonitorProperties.Thresholds t = properties.getThresholds();
        if (mq135 == null) {
            return "未知";
        }
        if (mq135 <= t.getMq135Warn()) {
            return "正常";
        }
        return "超标";
    }
}
