#include "sensor_frame.h"
#include <stdio.h>
#include <string.h>

int sensor_frame_json(const SensorFrame *f,
                      const char *device_id,
                      char *buf,
                      int buf_len)
{
    if (!f || !buf || buf_len <= 0) {
        return 0;
    }

    int n = snprintf(buf, (size_t)buf_len,
        "{"
        "\"deviceId\":\"%s\","
        "\"temperature\":%.1f,"
        "\"humidity\":%.1f,"
        "\"pir\":%u,"
        "\"mq135\":%.1f"
        "}",
        (device_id && device_id[0]) ? device_id : "sensor-001",
        (double)f->temperature,
        (double)f->humidity,
        (unsigned)f->pir,
        (double)f->mq135);

    if (n <= 0 || n >= buf_len) {
        return 0;
    }
    return n;
}
