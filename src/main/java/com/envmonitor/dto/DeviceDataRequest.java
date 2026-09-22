package com.envmonitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeviceDataRequest {

    @NotBlank(message = "deviceId 不能为空")
    private String deviceId;

    @NotNull(message = "temperature 不能为空")
    private Double temperature;

    @NotNull(message = "humidity 不能为空")
    private Double humidity;

    @NotNull(message = "pir 不能为空")
    @Min(value = 0, message = "pir 只能是 0 或 1")
    @Max(value = 1, message = "pir 只能是 0 或 1")
    /** 0=有人，1=无人（红外模块低电平有效） */
    private Integer pir;

    @NotNull(message = "mq135 不能为空")
    private Double mq135;

    private Long timestampMs;

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
    public Long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(Long timestampMs) { this.timestampMs = timestampMs; }
}
