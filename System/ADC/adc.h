#ifndef __ADC_H
#define __ADC_H

#include "stm32f10x.h"

void MY_ADC_Init(void);
u16 MY_ADC_GetValue(void);
u16 ADC_GetAvG_Value(void);

// MQ135传感器函数
u16 MQ135_GetRawData(void);
float MQ135_GetConcentrationPPM(void);



#endif
