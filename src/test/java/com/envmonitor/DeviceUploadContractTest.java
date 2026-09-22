package com.envmonitor;

import com.envmonitor.dto.DeviceDataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 硬件上报契约测试：Keil 固件 / serial_to_http.py 发送的 JSON 必须被接受。
 */
@SpringBootTest(properties = "env.monitor.simulate-enabled=false")
@AutoConfigureMockMvc
class DeviceUploadContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void hardwareJsonUploadIsAccepted() throws Exception {
        DeviceDataRequest req = new DeviceDataRequest();
        req.setDeviceId("sensor-001");
        req.setTemperature(25.3);
        req.setHumidity(58.0);
        req.setPir(0); // 0=有人
        req.setMq135(132.5);

        mockMvc.perform(post("/api/device/data")
                        .header("X-Device-Key", "env-monitor-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deviceId").value("sensor-001"))
                .andExpect(jsonPath("$.data.temperature").value(25.3))
                .andExpect(jsonPath("$.data.pir").value(0))
                .andExpect(jsonPath("$.data.presence").value(true))
                .andExpect(jsonPath("$.data.led").exists());

        mockMvc.perform(post("/api/device/led")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"sensor-001\",\"on\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.led").value(true));

        mockMvc.perform(get("/api/sensor/realtime").param("deviceId", "sensor-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasData").value(true))
                .andExpect(jsonPath("$.data.led").value(true))
                .andExpect(jsonPath("$.data.threshold.thresholds.tempMin").exists());
    }
}
