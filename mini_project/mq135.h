#ifndef MQ135_H
#define MQ135_H

#include <stdint.h>

void mq135_init(void);

/* 读取 ADC 原始值 */
uint16_t mq135_read_raw(void);

/* 换算相对浓度（简易映射，需按传感器标定） */
float mq135_to_ppm_like(uint16_t raw);

#endif /* MQ135_H */
