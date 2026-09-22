#ifndef PIR_H
#define PIR_H

#include <stdint.h>

void pir_init(void);

/* 1 = 有人，0 = 无人 */
uint8_t pir_read(void);

#endif /* PIR_H */
