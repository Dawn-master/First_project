# -*- coding: utf-8 -*-
"""写回 Keil 可用的 GBK 编码 main.c，恢复中文 OLED，并降低循环延时"""
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
    uint8_t local_alarm;
    uint16_t upload_tick = 0;

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
        DHT11_Read_Data(&temp, &humi);
        mq135 = MQ135_GetConcentrationPPM();
        hw = HW_Getstate();

        /* 本地阈值：温度>29 或 湿度>50 或 MQ>2000 */
        local_alarm = (temp > 29 || humi > 50 || mq135 > 2000) ? 1 : 0;

        /* OLED 中文显示（与原先布局一致） */
        OLED_ShowChinese(0, 0, "温度");
        OLED_ShowNum(40, 0, temp, 2, OLED_8X16);
        OLED_ShowString(56, 0, "C", OLED_8X16);

        OLED_ShowChinese(0, 16, "湿度");
        OLED_ShowNum(40, 16, humi, 2, OLED_8X16);
        OLED_ShowString(56, 16, "%", OLED_8X16);

        OLED_ShowString(0, 32, "mq135:", OLED_8X16);
        OLED_ShowNum(48, 32, (u16)mq135, 4, OLED_8X16);

        if(hw == 0)
            OLED_ShowChinese(0, 48, "有人");
        else
            OLED_ShowChinese(0, 48, "无人");

        if(local_alarm)
        {
            OLED_ShowString(40, 48, "warn", OLED_8X16);
        }
        else
        {
            OLED_ShowString(40, 48, "    ", OLED_8X16);
        }
        OLED_Update();

        /* 约每圈上报一次；Task 内已解析响应中的 led 并开关灯 */
        ESP8266_SetData((uint16_t)mq135, temp, humi, hw);
        ESP8266_Task();

        if(local_alarm)
        {
            LED_On();
            BEEP_On();
        }
        else
        {
            BEEP_Off();
            /* LED 由网页开关控制，已在 ESP8266_Task 内处理 */
        }

        upload_tick++;
        delay_ms(50);
    }
}
'''

path = Path(r"D:\keil5\STM32_Project1\text\User\main.c")
path.write_bytes(code.encode("gbk", errors="replace"))
print("main.c written GBK, bytes=", path.stat().st_size)
# 校验中文
raw = path.read_bytes()
print("has temp cn", "温度".encode("gbk") in raw)
print("has pir cn", "有人".encode("gbk") in raw)
