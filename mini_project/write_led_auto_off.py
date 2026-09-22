# -*- coding: utf-8 -*-
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

/*
 * LED 规则（和小程序约定）：
 * 1) 超标报警 → 立刻开灯 + 蜂鸣
 * 2) 报警结束 → 立刻关灯（不等网页）
 * 3) 未报警时 → 完全听小程序开关（led_cmd: 1开 / 0关）
 * 4) loop 只是循环计数，用来控制约 1 秒上报一次
 */
int main(void)
{
    uint16_t loop = 0;
    int led_cmd;
    uint8_t was_alarm = 0;
    uint8_t alarm;

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

        alarm = (temp > 30 || humi > 50 || mq135 > 200) ? 1 : 0;
        led_cmd = ESP8266_GetLedCmd();

        /* ---------- OLED ---------- */
        OLED_ShowChinese(0, 0, "温度");
        OLED_ShowNum(40, 0, temp, 2, OLED_8X16);
        OLED_ShowString(56, 0, "C", OLED_8X16);

        OLED_ShowChinese(0, 16, "湿度");
        OLED_ShowNum(40, 16, humi, 2, OLED_8X16);
        OLED_ShowString(56, 16, "%", OLED_8X16);

        OLED_ShowString(0, 32, "mq:", OLED_8X16);
        OLED_ShowNum(24, 32, (u16)mq135, 4, OLED_8X16);

        /* ---------- LED 控制 ---------- */
        if(alarm)
        {
            /* 1) 报警中：强制开灯 */
            LED_On();
            BEEP_On();
            was_alarm = 1;
            OLED_ShowString(40, 48, "warn", OLED_8X16);
        }
        else if(was_alarm)
        {
            /* 2) 刚从报警恢复：自动熄灭，再进入网页控制模式 */
            BEEP_Off();
            LED_Off();
            was_alarm = 0;
            /* 同步通知后端关灯，避免下次上报又带 true */
            /* 后端在 ingest 里也会在报警恢复时 setLed(false) */
            OLED_ShowString(40, 48, "off!", OLED_8X16);
        }
        else
        {
            /* 3) 正常：听小程序 */
            BEEP_Off();
            if(led_cmd == 1)
            {
                LED_On();
                OLED_ShowString(40, 48, "led+", OLED_8X16);
            }
            else
            {
                /* 0 或尚未收到指令( -1 ) 都按关灯处理 */
                LED_Off();
                OLED_ShowString(40, 48, (led_cmd == 0) ? "led-" : "led?", OLED_8X16);
            }
        }

        if(hw == 0)
            OLED_ShowChinese(0, 48, "有人");
        else
            OLED_ShowChinese(0, 48, "无人");

        OLED_Update();

        /* ---------- 约每 1 秒上报一次 ---------- */
        if((loop % 5) == 0)
        {
            ESP8266_SetData((uint16_t)mq135, temp, humi, hw);
            ESP8266_Task();
        }

        loop = (uint16_t)(loop + 1);
        delay_ms(200);
    }
}
'''

path = Path(r"D:\keil5\STM32_Project1\text\User\main.c")
path.write_bytes(code.encode("gbk", errors="replace"))
print("written", path.stat().st_size)
