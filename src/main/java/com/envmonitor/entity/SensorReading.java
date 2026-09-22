package com.envmonitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_reading", indexes = {
        @Index(name = "idx_sensor_device_time", columnList = "deviceId,recordedAt")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceId;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false)
    private Integer pir;

    @Column(nullable = false)
    private Double mq135;

    private Double comfortScore;

    @Column(nullable = false)
    private Boolean alerted = false;

    /** 北京时间 */
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Integer getPir() { return pir; }
    public void setPir(Integer pir) { this.pir = pir; }
    public Double getMq135() { return mq135; }
    public void setMq135(Double mq135) { this.mq135 = mq135; }
    public Double getComfortScore() { return comfortScore; }
    public void setComfortScore(Double comfortScore) { this.comfortScore = comfortScore; }
    public Boolean getAlerted() { return alerted; }
    public void setAlerted(Boolean alerted) { this.alerted = alerted; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
