#ifndef SENSOR_FRAME_H
#define SENSOR_FRAME_H

#include <stdint.h>

typedef struct {
    float    temperature;   /* ℃ */
    float    humidity;      /* % */
    uint8_t  pir;           /* 0/1 */
    uint16_t mq135_raw;     /* ADC 原始值，可选调试 */
    float    mq135;         /* 换算后的相对浓度 */
} SensorFrame;

/* 组一帧上报 JSON，写入 buf，返回长度（不含结尾 0） */
int sensor_frame_json(const SensorFrame *f,
                      const char *device_id,
                      char *buf,
                      int buf_len);

#endif /* SENSOR_FRAME_H */
