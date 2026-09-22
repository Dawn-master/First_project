#include "esp8266_http.h"
#include "board_config.h"
#include "stm32f1xx_hal.h"
#include <stdio.h>
#include <string.h>

/* USART2 — ESP8266 */
extern UART_HandleTypeDef huart2;
#define ESP_UART            &huart2
#define ESP_RX_BUF_SIZE     512

static uint8_t rx_buf[ESP_RX_BUF_SIZE];

static void esp_flush_rx(void)
{
    memset(rx_buf, 0, sizeof(rx_buf));
}

/* 简单等待模块回传关键字 */
static int esp_wait_ok(uint32_t timeout_ms)
{
    uint32_t tick = HAL_GetTick();
    uint16_t idx = 0;
    memset(rx_buf, 0, sizeof(rx_buf));

    while ((HAL_GetTick() - tick) < timeout_ms) {
        uint8_t ch;
        if (HAL_UART_Receive(ESP_UART, &ch, 1, 20) == HAL_OK) {
            if (idx < ESP_RX_BUF_SIZE - 1) {
                rx_buf[idx++] = ch;
                rx_buf[idx] = 0;
            }
            if (strstr((char *)rx_buf, "OK") || strstr((char *)rx_buf, "SEND OK")) {
                return 0;
            }
            if (strstr((char *)rx_buf, "ERROR") || strstr((char *)rx_buf, "FAIL")) {
                return -1;
            }
        }
    }
    return -2;
}

static int esp_cmd(const char *cmd, uint32_t timeout_ms)
{
    char line[256];
    snprintf(line, sizeof(line), "%s\r\n", cmd);
    esp_flush_rx();
    if (HAL_UART_Transmit(ESP_UART, (uint8_t *)line, (uint16_t)strlen(line), 500) != HAL_OK) {
        return -1;
    }
    return esp_wait_ok(timeout_ms);
}

int esp8266_http_init(void)
{
    /* 模块上电后建议延时 */
    HAL_Delay(1500);

    if (esp_cmd("AT", 2000) != 0) return -1;
    if (esp_cmd("ATE0", 2000) != 0) return -1;
    if (esp_cmd("AT+CWMODE=1", 3000) != 0) return -1;

    char cwjap[128];
    snprintf(cwjap, sizeof(cwjap), "AT+CWJAP=\"%s\",\"%s\"",
             WIFI_SSID, WIFI_PASSWORD);
    if (esp_cmd(cwjap, 15000) != 0) return -2;

    if (esp_cmd("AT+CIPMUX=0", 3000) != 0) return -3;
    return 0;
}

int esp8266_http_post_json(const char *json)
{
    if (!json) {
        return -1;
    }

    char body[512];
    char header[384];
    int body_len = (int)strlen(json);

    /* 带自定义 Header 的 HTTP/1.1 */
    int h = snprintf(header, sizeof(header),
        "POST %s HTTP/1.1\r\n"
        "Host: %s:%d\r\n"
        "Content-Type: application/json\r\n"
        "X-Device-Key: %s\r\n"
        "Content-Length: %d\r\n"
        "Connection: close\r\n"
        "\r\n",
        HTTP_PATH, SERVER_HOST, SERVER_PORT, DEVICE_KEY, body_len);

    if (h <= 0 || h >= (int)sizeof(header)) {
        return -2;
    }

    /* CIPSEND 只发 header，随后用 CIPMODE/多段发送；此处用一次性拼接简化 */
    size_t total = (size_t)h + (size_t)body_len;
    if (total > sizeof(body) + sizeof(header)) {
        return -3;
    }

    char cipsend[48];
    snprintf(cipsend, sizeof(cipsend), "AT+CIPSTART=\"TCP\",\"%s\",%d",
             SERVER_HOST, SERVER_PORT);
    if (esp_cmd(cipsend, 8000) != 0) {
        return -4;
    }

    snprintf(cipsend, sizeof(cipsend), "AT+CIPSEND=%u", (unsigned)total);
    if (esp_cmd(cipsend, 3000) != 0) {
        return -5;
    }

    /* 在收到 '>' 后发送数据：多数固件 AT+CIPSEND 返回 OK 后可直接发 */
    if (HAL_UART_Transmit(ESP_UART, (uint8_t *)header, (uint16_t)h, 2000) != HAL_OK) {
        return -6;
    }
    if (HAL_UART_Transmit(ESP_UART, (uint8_t *)json, (uint16_t)body_len, 2000) != HAL_OK) {
        return -7;
    }

    if (esp_wait_ok(8000) != 0) {
        return -8;
    }

    esp_cmd("AT+CIPCLOSE", 2000);
    return 0;
}
