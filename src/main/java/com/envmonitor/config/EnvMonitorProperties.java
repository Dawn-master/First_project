package com.envmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "env.monitor")
public class EnvMonitorProperties {

    private boolean simulateEnabled = false;
    private long simulateIntervalMs = 3000;
    private String defaultDeviceId = "sensor-001";
    private String deviceApiKey = "env-monitor-2026";
    private Thresholds thresholds = new Thresholds();

    public boolean isSimulateEnabled() { return simulateEnabled; }
    public void setSimulateEnabled(boolean simulateEnabled) { this.simulateEnabled = simulateEnabled; }
    public long getSimulateIntervalMs() { return simulateIntervalMs; }
    public void setSimulateIntervalMs(long simulateIntervalMs) { this.simulateIntervalMs = simulateIntervalMs; }
    public String getDefaultDeviceId() { return defaultDeviceId; }
    public void setDefaultDeviceId(String defaultDeviceId) { this.defaultDeviceId = defaultDeviceId; }
    public String getDeviceApiKey() { return deviceApiKey; }
    public void setDeviceApiKey(String deviceApiKey) { this.deviceApiKey = deviceApiKey; }
    public Thresholds getThresholds() { return thresholds; }
    public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }

    /** 与硬件一致：只判超过上限（temp>30、humi>50、mq135>200） */
    public static class Thresholds {
        private double tempMax = 30.0;
        private double humidityMax = 50.0;
        private double mq135Warn = 200.0;
        private double mq135Danger = 200.0;
        /* 兼容旧字段，报警逻辑不再使用下限 */
        private double tempMin = -1000.0;
        private double humidityMin = -1000.0;

        public double getTempMin() { return tempMin; }
        public void setTempMin(double tempMin) { this.tempMin = tempMin; }
        public double getTempMax() { return tempMax; }
        public void setTempMax(double tempMax) { this.tempMax = tempMax; }
        public double getHumidityMin() { return humidityMin; }
        public void setHumidityMin(double humidityMin) { this.humidityMin = humidityMin; }
        public double getHumidityMax() { return humidityMax; }
        public void setHumidityMax(double humidityMax) { this.humidityMax = humidityMax; }
        public double getMq135Warn() { return mq135Warn; }
        public void setMq135Warn(double mq135Warn) { this.mq135Warn = mq135Warn; }
        public double getMq135Danger() { return mq135Danger; }
        public void setMq135Danger(double mq135Danger) { this.mq135Danger = mq135Danger; }
    }
}
