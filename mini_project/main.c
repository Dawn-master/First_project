/**
 * 多传感器环境监测 — Keil/STM32 主循环骨架
 *
 * 集成步骤：
 * 1. CubeMX 生成工程：USART1(日志) + USART2(ESP8266) + ADC1 + 对应 GPIO
 * 2. 将本目录 Src/Inc 加入 Keil 工程
 * 3. 在 CubeMX 用户代码区调用 app_setup()/app_loop()，
 *    或直接使用本文件作为 main.c 并保留 HAL 初始化片段
 */
#include "stm32f1xx_hal.h"
#include "board_config.h"
#include "dht11.h"
#include "mq135.h"
#include "pir.h"
#include "sensor_frame.h"
#include "esp8266_http.h"
#include <stdio.h>
#include <string.h>

/* CubeMX 生成的句柄（名称需一致） */
extern UART_HandleTypeDef huart1;   /* 日志 */
extern UART_HandleTypeDef huart2;   /* ESP8266 */
extern ADC_HandleTypeDef  hadc1;

static char json_buf[256];
static uint32_t last_sample_tick;
static uint32_t last_upload_tick;

#ifdef __GNUC__
int __io_putchar(int ch)
#else
int fputc(int ch, FILE *f)
#endif
{
    HAL_UART_Transmit(&huart1, (uint8_t *)&ch, 1, 10);
    return ch;
}

static void uart_log(const char *s)
{
    HAL_UART_Transmit(&huart1, (uint8_t *)s, (uint16_t)strlen(s), 200);
}

void app_setup(void)
{
    /* 若使用 CubeMX，此处前应已完成 HAL_Init + 外设初始化 */
    pir_init();
    mq135_init();

    uart_log("\r\n[EnvMonitor] boot\r\n");

#if USE_ESP8266_HTTP
    if (esp8266_http_init() == 0) {
        uart_log("[ESP8266] WiFi OK\r\n");
    } else {
        uart_log("[ESP8266] WiFi FAIL, serial-only mode\r\n");
    }
#endif

    last_sample_tick = HAL_GetTick();
    last_upload_tick = HAL_GetTick();
}

int app_sample(SensorFrame *frame)
{
    float t = 0, h = 0;
    int ret = dht11_read(&t, &h);
    if (ret != 0) {
        /* 读失败保持上次/默认值，避免上传 NaN */
        static float last_t = 25.0f;
        static float last_h = 50.0f;
        t = last_t;
        h = last_h;
        uart_log("[DHT11] read fail, reuse last\r\n");
    } else {
        /* 成功则更新缓存（函数外也可用 static 保存） */
    }

    uint16_t raw = mq135_read_raw();
    frame->temperature = t;
    frame->humidity = h;
    frame->pir = pir_read();
    frame->mq135_raw = raw;
    frame->mq135 = mq135_to_ppm_like(raw);
    return ret;
}

void app_loop_once(void)
{
    uint32_t now = HAL_GetTick();
    static SensorFrame frame = {25.0f, 50.0f, 0, 0, 120.0f};

    if ((now - last_sample_tick) >= SAMPLE_INTERVAL_MS) {
        last_sample_tick = now;
        app_sample(&frame);

        int n = sensor_frame_json(&frame, DEVICE_ID, json_buf, (int)sizeof(json_buf));
        if (n > 0) {
            uart_log(json_buf);
            uart_log("\r\n");
        }
    }

    if ((now - last_upload_tick) >= HTTP_UPLOAD_MS) {
        last_upload_tick = now;

        if (sensor_frame_json(&frame, DEVICE_ID, json_buf, (int)sizeof(json_buf)) <= 0) {
            return;
        }

#if USE_ESP8266_HTTP
        int post = esp8266_http_post_json(json_buf);
        if (post == 0) {
            uart_log("[HTTP] upload OK\r\n");
        } else {
            char msg[64];
            snprintf(msg, sizeof(msg), "[HTTP] upload fail %d\r\n", post);
            uart_log(msg);
        }
#else
        uart_log("[SERIAL] JSON ready for serial_to_http.py\r\n");
#endif
    }
}

/* ---- 若本文件作为完整 main.c 使用，取消下面注释并删除工程自带 main ---- */
/*
int main(void)
{
    HAL_Init();
    SystemClock_Config();   // CubeMX 生成
    MX_GPIO_Init();
    MX_USART1_UART_Init();
    MX_USART2_UART_Init();
    MX_ADC1_Init();

    app_setup();
    while (1) {
        app_loop_once();
    }
}
*/
