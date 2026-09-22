package com.envmonitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 红外人员进出事件：PIR 0=有人 → ENTER，1=无人 → EXIT
 */
@Entity
@Table(name = "presence_event", indexes = {
        @Index(name = "idx_event_device_time", columnList = "deviceId,eventTime")
})
public class PresenceEvent {

    public static final String ENTER = "ENTER";
    public static final String EXIT = "EXIT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceId;

    /** ENTER / EXIT */
    @Column(nullable = false, length = 16)
    private String eventType;

    /** ENTER=有人进入房间，EXIT=离开房间 */
    @Column(nullable = false, length = 64)
    private String label;

    private Double temperature;
    private Double humidity;
    private Double mq135;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Double getMq135() { return mq135; }
    public void setMq135(Double mq135) { this.mq135 = mq135; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
