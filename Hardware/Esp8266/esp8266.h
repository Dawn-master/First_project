#ifndef __ESP8266_H
#define __ESP8266_H

#include "stm32f10x.h"
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include "delay.h"

/* 网络配置：与电脑当前 WLAN IPv4 必须一致，换网后改 IP 并重新编译下载 */
#define WIFI_SSID       "vivo S19dyr"
#define WIFI_PASSWORD   "dyr522521"
#define SERVER_IP       "10.211.22.28"
#define SERVER_PORT     "8080"
#define DEVICE_ID       "sensor-001"
#define DEVICE_KEY      "env-monitor-2026"

/* 上报周期建议 2s，兼顾稳定与实时（main.c 中 delay 控制） */
#define UPLOAD_PERIOD_MS  2000

void ESP8266_Init(void);
void ESP8266_Task(void);
void ESP8266_SetData(uint16_t mq135, uint8_t temp, uint8_t humi, uint8_t hw);
char ESP8266_GetCmd(void);
int  ESP8266_PollLed(void);
int  ESP8266_NetOk(void);
/* -1=未知 0=关灯 1=开灯；由上报/GET 解析结果写入，供 main 每个循环执行 */
int  ESP8266_GetLedCmd(void);

#endif
