#include "esp8266.h"
#include "led.h"

/* ---- 稳定上报策略 ----
 * 1) 每次 HTTP 前必定重新 TCP 连接（服务端 Connection: close 会关掉链路）
 * 2) 失败自动重试 2 次
 * 3) 连续失败多次则重连 WiFi / 复位 ESP8266
 * 4) 以 HTTP 响应中 "code":0 或 "led": 为成功标志
 */

#define RX_BUF_SIZE     1024
#define RETRY_MAX       2
#define FAIL_RST_LIMIT  5

static char rx_buf[RX_BUF_SIZE];
static uint16_t rx_index = 0;
static char s_rx_cmd = 0;

static uint16_t s_mq135 = 0;
static uint8_t s_humi = 0;
static uint8_t s_temp = 0;
static uint8_t s_hw = 0;

static uint8_t fail_streak = 0;
static uint8_t s_net_ok = 0;
/* 远程 LED 指令：-1未知 0关 1开；main 每个 while 循环读取并写 GPIO */
static int s_led_cmd = -1;

void USART1_IRQHandler(void)
{
    if(USART_GetITStatus(USART1, USART_IT_RXNE) != RESET)
    {
        char c = USART_ReceiveData(USART1);
        if(c == 'a' || c == 'b') s_rx_cmd = c;
        if(rx_index < RX_BUF_SIZE - 1)
        {
            rx_buf[rx_index++] = c;
            rx_buf[rx_index] = 0;
        }
        USART_ClearITPendingBit(USART1, USART_IT_RXNE);
    }
}

static void clear_buf(void)
{
    memset(rx_buf, 0, sizeof(rx_buf));
    rx_index = 0;
}

static int buf_has(const char *s)
{
    return (s && strstr(rx_buf, s) != NULL) ? 1 : 0;
}

static int wait_any(const char *a, const char *b, uint32_t timeout_ms)
{
    uint32_t t = 0;
    while(t < timeout_ms)
    {
        if(a && buf_has(a)) return 1;
        if(b && buf_has(b)) return 1;
        delay_ms(10);
        t += 10;
    }
    return 0;
}

static void send_byte(uint8_t b)
{
    while(USART_GetFlagStatus(USART1, USART_FLAG_TXE) == RESET);
    USART_SendData(USART1, b);
}

static void send_str(const char *s)
{
    while(*s) send_byte((uint8_t)*s++);
}

static void send_cmd(const char *cmd)
{
    send_str(cmd);
    send_str("\r\n");
}

static void uart_init(void)
{
    GPIO_InitTypeDef gpio;
    USART_InitTypeDef usart;
    NVIC_InitTypeDef nvic;

    RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1 | RCC_APB2Periph_GPIOA, ENABLE);

    gpio.GPIO_Pin = GPIO_Pin_9;
    gpio.GPIO_Mode = GPIO_Mode_AF_PP;
    gpio.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(GPIOA, &gpio);

    gpio.GPIO_Pin = GPIO_Pin_10;
    gpio.GPIO_Mode = GPIO_Mode_IN_FLOATING;
    GPIO_Init(GPIOA, &gpio);

    usart.USART_BaudRate = 115200;
    usart.USART_Mode = USART_Mode_Tx | USART_Mode_Rx;
    usart.USART_StopBits = USART_StopBits_1;
    usart.USART_WordLength = USART_WordLength_8b;
    usart.USART_Parity = USART_Parity_No;
    USART_Init(USART1, &usart);

    USART_ITConfig(USART1, USART_IT_RXNE, ENABLE);

    nvic.NVIC_IRQChannel = USART1_IRQn;
    nvic.NVIC_IRQChannelPreemptionPriority = 0;
    nvic.NVIC_IRQChannelSubPriority = 0;
    nvic.NVIC_IRQChannelCmd = ENABLE;
    NVIC_Init(&nvic);

    USART_Cmd(USART1, ENABLE);
}

static int wifi_join(void)
{
    char cmd[160];

    clear_buf();
    send_cmd("AT+CWMODE=1");
    delay_ms(400);

    /* 先查是否已连上，避免每次硬连 */
    clear_buf();
    send_cmd("AT+CIPSTATUS");
    if(wait_any("STATUS:2", "STATUS:3", 800) || wait_any("STATUS:4", "STATUS:5", 200))
    {
        /* 已有连接态/获得 IP，尝试仍执行一次 CWJAP 无妨 */
    }

    clear_buf();
    sprintf(cmd, "AT+CWJAP=\"%s\",\"%s\"", WIFI_SSID, WIFI_PASSWORD);
    send_cmd(cmd);
    if(!wait_any("WIFI CONNECTED", "OK", 15000))
    {
        clear_buf();
        send_cmd("AT+CWJAP?");
        if(wait_any(WIFI_SSID, "OK", 3000))
        {
            s_net_ok = 1;
            return 1;
        }
        s_net_ok = 0;
        return 0;
    }

    clear_buf();
    send_cmd("AT+CIFSR");
    wait_any("STAIP", "OK", 3000);
    s_net_ok = 1;
    return 1;
}

static void esp_soft_reset(void)
{
    clear_buf();
    send_cmd("AT+RST");
    delay_ms(2000);
    clear_buf();
    send_cmd("AT");
    wait_any("OK", "ready", 2000);
    clear_buf();
    send_cmd("ATE0");
    wait_any("OK", NULL, 800);
    wifi_join();
}

void ESP8266_Init(void)
{
    uart_init();
    delay_ms(800);
    clear_buf();

    send_cmd("AT");
    wait_any("OK", "ready", 1500);
    clear_buf();

    send_cmd("ATE0");
    wait_any("OK", NULL, 800);
    clear_buf();

    send_cmd("AT+CWMODE=1");
    delay_ms(500);
    clear_buf();

    send_cmd("AT+CIPMUX=0");
    delay_ms(300);
    clear_buf();

    wifi_join();
}

void ESP8266_SetData(uint16_t mq135, uint8_t temp, uint8_t humi, uint8_t hw)
{
    s_mq135 = mq135;
    s_humi = humi;
    s_temp = temp;
    s_hw = hw;
}

char ESP8266_GetCmd(void)
{
    char cmd = s_rx_cmd;
    s_rx_cmd = 0;
    return cmd;
}

int ESP8266_PollLed(void)
{
    if(buf_has("\"led\":true") || buf_has("\"led\": true")) return 1;
    if(buf_has("\"led\":false") || buf_has("\"led\": false")) return 0;
    return -1;
}

/* 只要响应里有 led 字段就执行 GPIO，禁止“识别到却不动作” */
static int apply_led_from_buf(void)
{
    int led = ESP8266_PollLed();
    if(led == 1) { s_led_cmd = 1; LED_On(); return 1; }
    if(led == 0) { s_led_cmd = 0; LED_Off(); return 1; }
    return 0;
}

int ESP8266_GetLedCmd(void)
{
    return s_led_cmd;
}

int ESP8266_NetOk(void)
{
    return s_net_ok;
}

static int tcp_connect(void)
{
    char cmd[96];

    clear_buf();
    send_cmd("AT+CIPCLOSE");
    wait_any("OK", "ERROR", 800);

    clear_buf();
    sprintf(cmd, "AT+CIPSTART=\"TCP\",\"%s\",%s", SERVER_IP, SERVER_PORT);
    send_cmd(cmd);
    if(wait_any("CONNECT", "OK", 6000))
    {
        if(buf_has("ERROR") || buf_has("FAIL"))
            return 0;
        return 1;
    }
    return 0;
}

/* 后备：单独 GET /api/device/led，保证网页开关能作用到灯 */
static int get_led_http(void)
{
    char http[280];
    char cmd[48];
    int t;

    if(!tcp_connect())
        return 0;

    clear_buf();
    sprintf(http,
            "GET /api/device/led?deviceId=%s HTTP/1.1\r\n"
            "Host: %s:%s\r\n"
            "X-Device-Key: %s\r\n"
            "Connection: close\r\n"
            "\r\n",
            DEVICE_ID, SERVER_IP, SERVER_PORT, DEVICE_KEY);

    sprintf(cmd, "AT+CIPSEND=%d", (int)strlen(http));
    send_cmd(cmd);
    if(!wait_any(">", "ERROR", 2500))
        return 0;

    send_str(http);

    for(t = 0; t < 60; t++)
    {
        if(apply_led_from_buf())
            return 1;
        if(buf_has("ERROR") || buf_has("FAIL"))
            return 0;
        delay_ms(10);
    }
    return 0;
}

static int http_post_once(void)
{
    char json[160];
    char http[480];
    char cmd[48];
    int body_len, total, t;

    if(!tcp_connect())
        return 0;

    clear_buf();
    sprintf(json,
            "{\"deviceId\":\"%s\",\"temperature\":%d,\"humidity\":%d,\"pir\":%u,\"mq135\":%u}",
            DEVICE_ID, (int)s_temp, (int)s_humi, (unsigned)s_hw, (unsigned)s_mq135);
    body_len = (int)strlen(json);

    sprintf(http,
            "POST /api/device/data HTTP/1.1\r\n"
            "Host: %s:%s\r\n"
            "Content-Type: application/json\r\n"
            "X-Device-Key: %s\r\n"
            "Content-Length: %d\r\n"
            "Connection: close\r\n"
            "\r\n"
            "%s",
            SERVER_IP, SERVER_PORT, DEVICE_KEY, body_len, json);
    total = (int)strlen(http);

    clear_buf();
    sprintf(cmd, "AT+CIPSEND=%d", total);
    send_cmd(cmd);
    if(!wait_any(">", "ERROR", 2500))
        return 0;

    send_str(http);

    /*
     * 注意：响应 JSON 里 "led" 在 "data" 深处，往往晚于 "code":0 出现。
     * 不能一看到 code:0 就 return，否则小程序开关永远同步不到灯。
     */
    for(t = 0; t < 80; t++)
    {
        apply_led_from_buf();

        if(buf_has("\"led\":true") || buf_has("\"led\": true")
           || buf_has("\"led\":false") || buf_has("\"led\": false"))
        {
            apply_led_from_buf();
            return 1;
        }

        if(buf_has("\"code\":0") || buf_has("\"code\": 0"))
        {
            /* 已看到业务成功，再等最多 ~300ms 让 led 字段收完 */
            int w;
            for(w = 0; w < 30; w++)
            {
                if(apply_led_from_buf())
                    return 1;
                delay_ms(10);
            }
            return 1; /* 应用不到也返回成功，由 GET 兜底 */
        }

        if(buf_has("ERROR") || buf_has("FAIL"))
        {
            if(apply_led_from_buf())
                return 1;
            return 0;
        }

        if(buf_has("SEND OK") && t > 25 && buf_has("HTTP/1.1"))
        {
            apply_led_from_buf();
            return 1;
        }
        delay_ms(10);
    }
    return 0;
}

/* 上报 + 强制同步 LED（每次任务都 GET 一次，保证小程序能控灯） */
void ESP8266_Task(void)
{
    int attempt;

    for(attempt = 0; attempt <= RETRY_MAX; attempt++)
    {
        if(http_post_once())
        {
            fail_streak = 0;
            s_net_ok = 1;
            clear_buf();
            /* 每次上报成功后再 GET /led，确保网页开关立即生效 */
            get_led_http();
            clear_buf();
            return;
        }
        delay_ms(150);
    }

    /* 上报失败也拉 LED */
    get_led_http();
    clear_buf();

    fail_streak++;

    if(fail_streak >= FAIL_RST_LIMIT)
    {
        esp_soft_reset();
        fail_streak = 0;
    }
    else
    {
        wifi_join();
    }
}
