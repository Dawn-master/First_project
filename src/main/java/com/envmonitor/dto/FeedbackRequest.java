package com.envmonitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @NotBlank(message = "content 不能为空")
    @Size(max = 500, message = "content 最多 500 字")
    private String content;

    /** REPAIR / SUGGEST / OTHER，默认 REPAIR */
    private String type = "REPAIR";

    private String contact = "";

    private String deviceId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
