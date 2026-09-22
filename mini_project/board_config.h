#ifndef BOARD_CONFIG_H
#define BOARD_CONFIG_H

/* ===== 设备身份（与后端一致） ===== */
#define DEVICE_ID           "sensor-001"
#define DEVICE_KEY          "env-monitor-2026"

/* ===== WiFi / 后端（改这里） ===== */
#define WIFI_SSID           "YourWiFi"
#define WIFI_PASSWORD       "YourPassword"
#define SERVER_HOST         "192.168.1.100"   /* 电脑局域网 IPv4 */
#define SERVER_PORT         8080
#define HTTP_PATH           "/api/device/data"
#define HTTP_UPLOAD_MS      3000UL            /* 上报周期 */

/* ===== 引脚（STM32F103 示例，按 CubeMX 调整） =====
 * 使用 HAL 时请保证这些宏与 CubeMX 生成的句柄一致：
 *  - DHT11: GPIOB, GPIO_PIN_12
 *  - PIR:   GPIOB, GPIO_PIN_13
 *  - MQ135: hadc1, CHANNEL_xxx
 */

#define SAMPLE_INTERVAL_MS  2000UL            /* 本地采样周期 */

/* 无 ESP8266 时置 0：仅串口打印 JSON，由 tools/serial_to_http.py 转发 */
#define USE_ESP8266_HTTP    1

#endif /* BOARD_CONFIG_H */
