# Keil 固件骨架 — 传感器采集 + 串口/HTTP 上报

面向 STM32（Keil MDK / HAL），对接本项目 JavaWeb 后端。

## 采集与上报链路

```
DHT11 ──温湿度──┐
PIR  ──GPIO────┼──► STM32 主循环 ──组帧 JSON──┬── UART1 日志/透传
MQ135 ─ADC─────┘                              ├── ESP8266 HTTP POST
                                              └──（或）PC 串口转 HTTP 脚本
                                                      │
                                                      ▼
                                          http://<PC_IP>:8080/api/device/data
```

## 推荐硬件接线（STM32F103C8T6）

| 模块 | 引脚 | 说明 |
|------|------|------|
| DHT11 DATA | PB12 | 单总线，接 4.7k~10k 上拉 |
| HC-SR501 PIR | PB13 | GPIO 输入，有人输出高 |
| MQ135 AO | PA1 (ADC1_CH1) | 模拟量；DO 可不用 |
| ESP8266 TX | PA3 (USART2_RX) | 模块→MCU |
| ESP8266 RX | PA2 (USART2_TX) | MCU→模块，建议电平匹配 |
| USB 串口日志 | PA9/PA10 (USART1) | 打印 JSON / AT 调试 |

供电：MQ135 需预热；VCC 5V，AO 分压到 ≤3.3V 再进 ADC。

## 目录

```
firmware/
├── README.md
├── Inc/
│   ├── board_config.h      # 引脚、WiFi、服务器地址
│   ├── dht11.h
│   ├── mq135.h
│   ├── pir.h
│   ├── sensor_frame.h
│   └── esp8266_http.h
├── Src/
│   ├── main.c              # 主循环骨架
│   ├── dht11.c
│   ├── mq135.c
│   ├── pir.c
│   ├── sensor_frame.c      # 组 JSON
│   └── esp8266_http.c      # AT 指令 HTTP POST
└── tools/
    └── serial_to_http.py   # 无 ESP8266 时：串口 JSON → 后端 HTTP
```

## 在 Keil 中怎么用

1. CubeMX/Keil 新建 STM32F103 工程，开启：
   - `USART1` 115200（日志）
   - `USART2` 115200（ESP8266）
   - `ADC1` + 通道对应 MQ135
   - SysTick 或 HAL 时基
2. 把 `Inc/`、`Src/` 源文件加入工程（覆盖/替换你自己 `main.c` 时注意保留 HAL 初始化段）。
3. 改 `Inc/board_config.h`：
   - `WIFI_SSID` / `WIFI_PASSWORD`
   - `SERVER_HOST`（电脑局域网 IP，如 `192.168.1.100`）
   - `SERVER_PORT`（默认 `8080`）
   - `DEVICE_ID` / `DEVICE_KEY`
4. 编译下载，打开串口助手应周期打印 JSON。

## 与后端约定

`POST /api/device/data`  
Header：`X-Device-Key: env-monitor-2026`  
Body：

```json
{
  "deviceId": "sensor-001",
  "temperature": 25.3,
  "humidity": 58.0,
  "pir": 1,
  "mq135": 132.5
}
```

`pir`: `0` 无人 / `1` 有人。  
MQ135 为 ADC 换算后的相对值（演示用，需标定）。

## 两种上报方式

### A. ESP8266 直接 HTTP（推荐答辩演示）

固件 `esp8266_http.c` 用 AT 指令连 WiFi，对后端 POST。

### B. 串口 JSON + 电脑转发（无 WiFi 模块时）

MCU 只往 USART1 打 JSON 行，电脑运行：

```powershell
python firmware\tools\serial_to_http.py --port COM3 --host 127.0.0.1 --key env-monitor-2026
```

脚本读串口、解析 JSON、转发到后端。

## 标定提醒

- DHT11：读失败重试，不要在中断里做长延时阻塞过久。
- MQ135：上电预热 1~2 分钟；清洁空气下取基准，演示时说明是相对浓度。
- PIR：模块自带延时，读 GPIO 高低电平即可。

## 联调检查清单

1. 后端已启动，`GET /api/health` 返回 UP  
2. 电脑与开发板/手机同一局域网  
3. `board_config.h` 中 IP 为电脑真实 IPv4（不是 127.0.0.1，除非转发脚本跑在同一台机）  
4. 串口能看到 JSON；后端 `/api/sensor/latest` 数值变化  
5. 小程序或 `index.html` 刷新可见
