#ifndef ESP8266_HTTP_H
#define ESP8266_HTTP_H

#include <stdint.h>

/* 初始化 UART2 + ESP8266 连 WiFi。0 成功 */
int esp8266_http_init(void);

/* HTTP POST JSON 到后端。0 成功 */
int esp8266_http_post_json(const char *json);

#endif /* ESP8266_HTTP_H */
