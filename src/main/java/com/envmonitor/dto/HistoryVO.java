package com.envmonitor.dto;

import com.envmonitor.entity.SensorReading;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史数据：stats 为区间全量统计；points 为曲线抽样点（可能少于 stats.count）
 */
public record HistoryVO(
        String deviceId,
        int hours,
        int count,
        Stats stats,
        List<Point> points
) {

    /** 区间统计（来自数据库聚合，不是只对曲线点统计） */
    public record Stats(
            long totalCount,
            Double avgTemperature,
            Double avgHumidity,
            Double avgMq135,
            Double maxMq135,
            Double minMq135,
            Double presenceRate
    ) {
    }

    public record Point(
            LocalDateTime time,
            Double temperature,
            Double humidity,
            Integer pir,
            Double mq135,
            Double comfortScore
    ) {
        public static Point from(SensorReading r) {
            return new Point(
                    r.getRecordedAt(),
                    r.getTemperature(),
                    r.getHumidity(),
                    r.getPir(),
                    r.getMq135(),
                    r.getComfortScore()
            );
        }
    }

    public static HistoryVO of(String deviceId, int hours, Stats stats, List<SensorReading> readings) {
        List<Point> points = new ArrayList<>();
        if (readings != null) {
            for (SensorReading r : readings) {
                points.add(Point.from(r));
            }
        }
        int shown = points.size();
        int total = stats != null ? (int) stats.totalCount() : shown;
        return new HistoryVO(deviceId, hours, total, stats, points);
    }
}
