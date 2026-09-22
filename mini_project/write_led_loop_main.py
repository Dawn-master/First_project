# -*- coding: utf-8 -*-
"""main.c：解释 loop + 每个循环都执行 LED 指令"""
from pathlib import Path

code = r'''#include "stm32f10x.h"
#include "delay.h"
#include "led.h"
#include "oled.h"
#include "HW.h"
#include "dht11.h"
#include "adc.h"
#include "esp8266.h"

uint8_t hw;
uint8_t temp, humi;
float mq135;

int main(void)
{
    /*
     * loop = 循环次数计数器（不是网络协议里的“循环”）
     * 每 while 一圈加 1；本程序 delay_ms(200)，所以 5 圈 ≈ 1 秒
     * 用它控制“多久上报一次网络”，避免每 0.2 秒都打 TCP
     */
    uint16_t loop = 0;
    int led_cmd;

    SystemInit();
    delay_init(72);
    LED_Init();
    BEEP_Init();
    OLED_Init();
    OLED_Clear();
    DHT11_Init();
    MY_ADC_Init();
    HW_Init();
    ESP8266_Init();

    while(1)
    {
        /* ========== 1) 本地采样（每圈都做） ========== */
        DHT11_Read_Data(&temp, &humi);
        mq135 = MQ135_GetConcentrationPPM();
        hw = HW_Getstate();

        /* ========== 2) OLED 显示（每圈都刷） ========== */
        OLED_ShowChinese(0, 0, "温度");
        OLED_ShowNum(40, 0, temp, 2, OLED_8X16);
        OLED_ShowString(56, 0, "C", OLED_8X16);

        OLED_ShowChinese(0, 16, "湿度");
        OLED_ShowNum(40, 16, humi, 2, OLED_8X16);
        OLED_ShowString(56, 16, "%", OLED_8X16);

        OLED_ShowString(0, 32, "mq:", OLED_8X16);
        OLED_ShowNum(24, 32, (u16)mq135, 4, OLED_8X16);

        if(hw == 0)
            OLED_ShowChinese(0, 48, "有人");
        else
            OLED_ShowChinese(0, 48, "无人");

        /* ========== 3) LED：每个循环都根据网页指令执行 ========== */
        led_cmd = ESP8266_GetLedCmd(); /* -1未知 0关 1开 */

        if(temp > 30 || humi > 50 || mq135 > 200)
        {
            /* 本地报警优先：强制灯亮 */
            LED_On();
            BEEP_On();
            OLED_ShowString(40, 48, "warn", OLED_8X16);
        }
        else
        {
            BEEP_Off();
            /* 无本地报警：完全听小程序开关 */
            if(led_cmd == 1)
            {
                LED_On();
                OLED_ShowString(40, 48, "led+", OLED_8X16);
            }
            else if(led_cmd == 0)
            {
                LED_Off();
                OLED_ShowString(40, 48, "led-", OLED_8X16);
            }
            else
            {
                OLED_ShowString(40, 48, "led?", OLED_8X16);
            }
        }
        OLED_Update();

        /* ========== 4) 网络上报：每 5 圈 ≈ 1 秒 ========== */
        if((loop % 5) == 0)
        {
            ESP8266_SetData((uint16_t)mq135, temp, humi, hw);
            ESP8266_Task(); /* 内部会解析 led 并写入 s_led_cmd */
        }

        loop = (uint16_t)(loop + 1);
        delay_ms(200);
    }
}
'''

path = Path(r"D:\keil5\STM32_Project1\text\User\main.c")
path.write_bytes(code.encode("gbk", errors="replace"))
t = path.read_bytes().decode("gbk")
print("has GetLedCmd", "ESP8266_GetLedCmd" in t)
print("has loop comment", "loop = " in t)
print("bytes", path.stat().st_size)
