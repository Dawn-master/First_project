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

int main(void)
{
    uint8_t local_alarm;
    uint16_t loop = 0;

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
        {
            OLED_ShowString(40, 48, "warn", OLED_8X16);
        }
        else
        {
            OLED_ShowString(40, 48, ESP8266_NetOk() ? "net " : "wifi", OLED_8X16);
        }
        OLED_Update();

        if(local_alarm)
        {
            LED_On();
            BEEP_On();
        }
        else
        {
            BEEP_Off();
        }

        /* every 10 loops * 200ms = 2s upload */
        if(loop % 10 == 0)
        {
            ESP8266_SetData((uint16_t)mq135, temp, humi, hw);
            ESP8266_Task();
        }

        loop++;
        delay_ms(200);
    }
}
'''

path = Path(r"D:\keil5\STM32_Project1\text\User\main.c")
path.write_bytes(code.encode("gbk", errors="replace"))
print("main.c bytes", path.stat().st_size)
print(path.read_bytes().decode("gbk").count("delay_ms(200)"))
