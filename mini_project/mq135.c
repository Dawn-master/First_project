#include "mq135.h"
#include "stm32f1xx_hal.h"

/* 与 CubeMX 中 hadc1 对应；通道在 init 里配置 */
extern ADC_HandleTypeDef hadc1;

#define MQ135_ADC_CHANNEL   ADC_CHANNEL_1   /* PA1 */

/* 简易映射参数：按实际传感器/分压标定 */
#define MQ135_RAW_AIR       400.0f          /* 清洁空气附近 ADC */
#define MQ135_RAW_POLLUTE   1200.0f         /* 污染演示点 ADC */
#define MQ135_SCALE         400.0f          /* 映射到 0~400 量程风格 */

void mq135_init(void)
{
    /* 假设 CubeMX 已 MX_ADC1_Init()；此处只做校准 */
    HAL_ADCEx_Calibration_Start(&hadc1);
}

uint16_t mq135_read_raw(void)
{
    ADC_ChannelConfTypeDef sConfig = {0};
    sConfig.Channel = MQ135_ADC_CHANNEL;
#if defined(STM32F1xx) || defined(STM32F103xB)
    sConfig.Rank = 1; /* F1 HAL */
#else
    sConfig.Rank = ADC_REGULAR_RANK_1;
#endif
    sConfig.SamplingTime = ADC_SAMPLETIME_239CYCLES_5;
    if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK) {
        return 0;
    }

    HAL_ADC_Start(&hadc1);
    if (HAL_ADC_PollForConversion(&hadc1, 50) != HAL_OK) {
        HAL_ADC_Stop(&hadc1);
        return 0;
    }
    uint16_t raw = (uint16_t)HAL_ADC_GetValue(&hadc1);
    HAL_ADC_Stop(&hadc1);
    return raw;
}

float mq135_to_ppm_like(uint16_t raw)
{
    if (raw <= (uint16_t)MQ135_RAW_AIR) {
        return 50.0f + (float)raw * 0.05f;
    }
    float span = MQ135_RAW_POLLUTE - MQ135_RAW_AIR;
    if (span < 1.0f) {
        span = 1.0f;
    }
    float ratio = ((float)raw - MQ135_RAW_AIR) / span;
    if (ratio > 3.0f) {
        ratio = 3.0f;
    }
    return 80.0f + ratio * MQ135_SCALE;
}
