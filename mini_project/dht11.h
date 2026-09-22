#ifndef DHT11_H
#define DHT11_H

#include <stdint.h>

/* 返回 0 成功，非 0 失败 */
int dht11_read(float *temperature, float *humidity);

#endif /* DHT11_H */
