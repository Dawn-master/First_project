package com.envmonitor.service;

import com.envmonitor.config.EnvMonitorProperties;
import com.envmonitor.dto.SensorLatestVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时阈值报警 —— 与硬件固件同一套规则：
 *   温度 &gt; 30℃  或  湿度 &gt; 50%  或  MQ135 &gt; 200  → 报警
 * （只判“超过上限”，不判过冷/过干）
 */
@Service
public class ThresholdService {

    private final EnvMonitorProperties properties;

    public ThresholdService(EnvMonitorProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> evaluate(SensorLatestVO latest) {
        EnvMonitorProperties.Thresholds t = properties.getThresholds();
        List<String> alarms = new ArrayList<>();

        Double temp = latest == null ? null : latest.temperature();
        Double humidity = latest == null ? null : latest.humidity();
        Double mq = latest == null ? null : latest.mq135();

        Map<String, Object> tempM = upperOnly(temp, t.getTempMax(), "℃", "温度");
        Map<String, Object> humM = upperOnly(humidity, t.getHumidityMax(), "%", "湿度");
        Map<String, Object> mqM = upperOnly(mq, t.getMq135Warn(), "", "MQ135");

        if (Boolean.FALSE.equals(tempM.get("ok")) && tempM.get("message") != null) {
            alarms.add(String.valueOf(tempM.get("message")));
        }
        if (Boolean.FALSE.equals(humM.get("ok")) && humM.get("message") != null) {
            alarms.add(String.valueOf(humM.get("message")));
        }
        if (Boolean.FALSE.equals(mqM.get("ok")) && mqM.get("message") != null) {
            alarms.add(String.valueOf(mqM.get("message")));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("temperature", tempM);
        result.put("humidity", humM);
        result.put("mq135", mqM);
        result.put("alarms", alarms);
        result.put("alarm", !alarms.isEmpty());
        result.put("thresholds", Map.of(
                "tempMax", t.getTempMax(),
                "humidityMax", t.getHumidityMax(),
                "mq135Warn", t.getMq135Warn(),
                "tempRule", "温度 > " + t.getTempMax() + "℃",
                "humidityRule", "湿度 > " + t.getHumidityMax() + "%",
                "mq135Rule", "MQ135 > " + t.getMq135Warn()
        ));
        return result;
    }

    /** 仅上限：value > max 则报警 */
    private Map<String, Object> upperOnly(Double value, double max, String unit, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("max", max);
        m.put("rule", name + " > " + max + unit);
        if (value == null) {
            m.put("ok", false);
            m.put("level", "NONE");
            m.put("message", null);
            return m;
        }
        if (value > max) {
            m.put("ok", false);
            m.put("level", "WARN");
            m.put("message", String.format("%s %.1f%s 超过阈值 %.0f%s", name, value, unit, max, unit));
        } else {
            m.put("ok", true);
            m.put("level", "OK");
            m.put("message", null);
        }
        return m;
    }
}
