# -*- coding: utf-8 -*-
"""修复 LED：无本地报警时不要强制关灯；缩短主循环延时"""
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

        /* 与网页/后端一致：温度>30 或 湿度>50 或 MQ>200 */
        local_alarm = (temp > 30 || humi > 50 || mq135 > 200) ? 1 : 0;

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
            OLED_ShowString(40, 48, "warn", OLED_8X16);
        else
            OLED_ShowString(40, 48, "led ", OLED_8X16);
        OLED_Update();

        /* 上报传感器；HTTP 响应里带 "led":true/false，Task 内 LED_On/Off */
        ESP8266_SetData((uint16_t)mq135, temp, humi, hw);
        ESP8266_Task();

        if(local_alarm)
        {
            /* 本地报警：强制亮灯+蜂鸣 */
            LED_On();
            BEEP_On();
        }
        else
        {
            /* 无本地报警：保持网页远程控制的灯，禁止再 LED_Off() */
            BEEP_Off();
        }

        delay_ms(80);
    }
}
'''

path = Path(r"D:\keil5\STM32_Project1\text\User\main.c")
path.write_bytes(code.encode("gbk", errors="replace"))
raw = path.read_bytes()
text = raw.decode("gbk")
print("has LED_Off in else?", "LED_Off" in text.split("else")[-1] if "else" in text else "?")
print("delay 80?", "delay_ms(80)" in text)
print("alarm rules?", "temp > 30" in text and "mq135 > 200" in text)
print("bytes", path.stat().st_size)
