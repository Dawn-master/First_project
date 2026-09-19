#include "adc.h"
#include "delay.h"
#include "math.h"

// MQ135洁净空气中基准电阻R0，需要传感器充分预热后实际校准得到，单位kΩ
static float g_MQ135_R0 = 10.0f;

/**
 * @brief  获取ADC单次转换采样值
 * @note   通道：ADC1 Channel1，对应PA1引脚；增加超时保护，防止硬件异常死循环卡死
 * @retval ADC原始采样值 0~4095；超时发生直接返回0
 */
u16 MY_ADC_GetValue(void)
{
    u16 timeout = 0;   //转换超时计数器，防止EOC标志永远不到位造成死锁
    
    //配置规则通道：ADC1，通道1，序列第1个，采样时间239.5个ADC周期
    ADC_RegularChannelConfig(ADC1, ADC_Channel_1, 1, ADC_SampleTime_239Cycles5);
    ADC_SoftwareStartConvCmd(ADC1, ENABLE);      //软件触发，启动一次ADC转换
    
    //等待EOC转换结束标志置1
    while(!ADC_GetFlagStatus(ADC1, ADC_FLAG_EOC))
    {
        timeout++;
        if(timeout > 1000) return 0;  //超时退出，避免程序卡死，健壮性优化
    }
    
    return ADC_GetConversionValue(ADC1); //读取转换结果寄存器
}

/**
 * @brief  ADC1初始化函数
 * @note   独立模式、单次非扫描、软件触发；PA1模拟输入；上电执行ADC自校准
 */
void MY_ADC_Init(void)
{
    GPIO_InitTypeDef GPIO_InitStructure;
    ADC_InitTypeDef ADC_InitStructure;
    
    //开启GPIOA、ADC1外设时钟（APB2总线）
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA | RCC_APB2Periph_ADC1, ENABLE);
    //ADC时钟分频 PCLK2/6，保证ADCCLK不超过最大14MHz硬件限制
    RCC_ADCCLKConfig(RCC_PCLK2_Div6);
    
    //PA1设置为模拟输入模式，禁止上下拉，用于接收MQ135模拟输出AO
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AIN;
    GPIO_Init(GPIOA, &GPIO_InitStructure);
    
    //ADC参数配置
    ADC_InitStructure.ADC_Mode = ADC_Mode_Independent;              //独立ADC模式
    ADC_InitStructure.ADC_ScanConvMode = DISABLE;                    //关闭扫描，单通道采集
    ADC_InitStructure.ADC_ContinuousConvMode = DISABLE;             //关闭连续转换，单次触发单次转换
    ADC_InitStructure.ADC_ExternalTrigConv = ADC_ExternalTrigConv_None;//关闭硬件外部触发，只用软件触发
    ADC_InitStructure.ADC_DataAlign = ADC_DataAlign_Right;           //数据右对齐，计算简单直观
    ADC_InitStructure.ADC_NbrOfChannel = 1;                          //规则序列通道数目为1
    ADC_Init(ADC1, &ADC_InitStructure);
    
    ADC_Cmd(ADC1, ENABLE); //使能ADC外设
    
    //ADC上电自校准流程，降低硬件电容带来采样误差
    ADC_ResetCalibration(ADC1);
    while(ADC_GetResetCalibrationStatus(ADC1));
    ADC_StartCalibration(ADC1);
    while(ADC_GetCalibrationStatus(ADC1));
}

/**
 * @brief  ADC多次采样求平均值，抑制高频噪声
 * @retval 10次采样平均值，范围0~4095
 */
u16 ADC_GetAvG_Value(void)
{
    u32 temp_val = 0;
    u8 t;
    for(t = 0; t < 10; t++)
    {
        temp_val += MY_ADC_GetValue();
        delay_ms(5);
    }
    return temp_val / 10;
}

/**
 * @brief  获取MQ135经过平均滤波后的原始ADC采样值
 * @retval ADC原始数值（0‑4095）；返回0可作为传感器断线/异常判断依据
 */
u16 MQ135_GetRawData(void)
{
    u16 rawAdc = ADC_GetAvG_Value();
    return rawAdc;
}

/**
 * @brief  获取MQ135检测的有害气体浓度（PPM）
 * @note   硬件负载电阻RL=10KΩ；使用手册拟合公式；注意MQ‑135模块需要5V供电、充分预热
 * @retval 气体浓度ppm，上限限制1000ppm；返回0代表ADC读取出错
 */
float MQ135_GetConcentrationPPM(void)
{
    float tempData = 0;
    float Vol, RS, ppm;
    u16 rawAdc;
    u8 i;
    
    //再次做10次采样做滑动平均，进一步降低噪声
    for(i = 0; i < 10; i++)
    {
        tempData += MY_ADC_GetValue();
        delay_ms(5);
    }
    tempData /= 10;
    rawAdc = (u16)tempData;
    
    //判读：ADC为0，判定传感器断开或者ADC异常，直接返回0
    if(rawAdc == 0)
    {
        return 0.0f;
    }
    
    //将ADC数字量换算为电压，12位ADC满量程4096，参考电压3.3V
    Vol = (tempData * 3.3f / 4096.0f);
    
    //计算传感器气敏电阻Rs
    //公式 Rs = (VCC − Vout) × RL / Vout
    //RL=10kΩ为本代码对应的模块板载负载电阻
    RS = (3.3f - Vol) * 10.0f / Vol;
    
    //MQ135氨气拟合公式  ppm = 116.602 * (R0 / Rs)^1.722
    //注意！！之前旧代码公式是 (Rs/Ro)，现在是 R0/Rs，底数取反，指数也改变
    ppm = 116.602f * powf(g_MQ135_R0 / RS, 1.722f);
    
    //结果限幅，防止异常采样值造成输出爆炸
    if(ppm > 1000.0f)
    {
        ppm = 1000.0f;
    }
    
    return ppm;
}


