#include "pir.h"
#include "stm32f1xx_hal.h"

#define PIR_PORT    GPIOB
#define PIR_PIN     GPIO_PIN_13

void pir_init(void)
{
    __HAL_RCC_GPIOB_CLK_ENABLE();

    GPIO_InitTypeDef g = {0};
    g.Pin = PIR_PIN;
    g.Mode = GPIO_MODE_INPUT;
    g.Pull = GPIO_NOPULL;   /* HC-SR501 通常自带输出，按模块说明可改下拉 */
    HAL_GPIO_Init(PIR_PORT, &g);
}

uint8_t pir_read(void)
{
    return (HAL_GPIO_ReadPin(PIR_PORT, PIR_PIN) == GPIO_PIN_SET) ? 1U : 0U;
}
