#include "dht11.h"
#include "board_config.h"
#include "stm32f1xx_hal.h"

/* 单总线：PB12 — 与 board_config.h 说明一致 */
#define DHT11_PORT          GPIOB
#define DHT11_PIN           GPIO_PIN_12

#define DHT11_TIMEOUT_US    200

static void dht_pin_output(void)
{
    GPIO_InitTypeDef g = {0};
    g.Pin = DHT11_PIN;
    g.Mode = GPIO_MODE_OUTPUT_PP;
    g.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(DHT11_PORT, &g);
}

static void dht_pin_input(void)
{
    GPIO_InitTypeDef g = {0};
    g.Pin = DHT11_PIN;
    g.Mode = GPIO_MODE_INPUT;
    g.Pull = GPIO_PULLUP;
    HAL_GPIO_Init(DHT11_PORT, &g);
}

static void delay_us(uint32_t us)
{
    /* 调试用忙等；量产建议 DWT 或硬件定时器 */
    uint32_t cycles = us * (SystemCoreClock / 1000000U);
    uint32_t i;
    for (i = 0; i < cycles; i++) {
        __NOP();
    }
}

static int wait_level(GPIO_PinState level, uint32_t timeout_us)
{
    uint32_t t = timeout_us;
    while (HAL_GPIO_ReadPin(DHT11_PORT, DHT11_PIN) != level) {
        if (t-- == 0) {
            return -1;
        }
        delay_us(1);
    }
    return 0;
}

static int read_byte(uint8_t *out)
{
    uint8_t v = 0;
    for (int i = 0; i < 8; i++) {
        if (wait_level(GPIO_PIN_SET, DHT11_TIMEOUT_US) != 0) {
            return -1;
        }
        delay_us(40);
        v <<= 1;
        if (HAL_GPIO_ReadPin(DHT11_PORT, DHT11_PIN) == GPIO_PIN_SET) {
            v |= 0x01;
        }
        if (wait_level(GPIO_PIN_RESET, DHT11_TIMEOUT_US) != 0) {
            return -1;
        }
    }
    *out = v;
    return 0;
}

int dht11_read(float *temperature, float *humidity)
{
    uint8_t data[5] = {0};

    if (!temperature || !humidity) {
        return -1;
    }

    dht_pin_output();
    HAL_GPIO_WritePin(DHT11_PORT, DHT11_PIN, GPIO_PIN_RESET);
    HAL_Delay(18);
    HAL_GPIO_WritePin(DHT11_PORT, DHT11_PIN, GPIO_PIN_SET);
    delay_us(30);
    dht_pin_input();

    if (wait_level(GPIO_PIN_RESET, DHT11_TIMEOUT_US) != 0) return -2;
    if (wait_level(GPIO_PIN_SET, DHT11_TIMEOUT_US) != 0) return -3;
    if (wait_level(GPIO_PIN_RESET, DHT11_TIMEOUT_US) != 0) return -4;

    for (int i = 0; i < 5; i++) {
        if (read_byte(&data[i]) != 0) {
            return -5;
        }
    }

    uint8_t sum = (uint8_t)(data[0] + data[1] + data[2] + data[3]);
    if (sum != data[4]) {
        return -6;
    }

    /* DHT11：整数部分有效；小数位常为 0 */
    *humidity = (float)data[0] + (float)data[1] * 0.1f;
    *temperature = (float)data[2] + (float)data[3] * 0.1f;
    return 0;
}
