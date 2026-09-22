package com.envmonitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 设备报修 / 反馈 */
@Entity
@Table(name = "feedback", indexes = {
        @Index(name = "idx_feedback_device_time", columnList = "deviceId,createdAt")
})
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceId;

    /** REPAIR 故障报修 / SUGGEST 建议 / OTHER 其他 */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 20)
    private String contact;

    @Column(nullable = false, length = 500)
    private String content;

    /** OPEN 处理中 / DONE 已处理 */
    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
