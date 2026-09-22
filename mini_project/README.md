# MDK-ARM / Keil 集成说明

本目录不提供完整 `.uvprojx`（与 Keil 版本/CubeMX 芯片包强绑定），请按下述步骤挂进你自己的工程。

## 1. CubeMX 建议配置（STM32F103C8T6）

| 外设 | 配置 |
|------|------|
| USART1 | 异步 115200，日志 |
| USART2 | 异步 115200，ESP8266 |
| ADC1 | IN1（PA1），扫描关闭 |
| GPIO | PB12 输出（DHT11，代码里会切换模式）；PB13 输入（PIR） |
| SYS | Timebase = SysTick |

生成工具链选 **MDK-ARM**，生成后用 Keil 打开。

## 2. 加入源文件

Keil 工程管理：

1. 新建 Group：`Firmware`
2. 添加文件：
   - `../Src/main.c`（若与 CubeMX 的 main.c 冲突：改名 `app_task.c`，在 CubeMX main 的 while(1) 里调用 `app_loop_once()`，在初始化后调用 `app_setup()`）
   - `../Src/dht11.c`
   - `../Src/mq135.c`
   - `../Src/pir.c`
   - `../Src/sensor_frame.c`
   - `../Src/esp8266_http.c`
3. C/C++ → Include Paths 增加 `../Inc`

## 3. 与 CubeMX main.c 的推荐接法（更稳）

不替换 `main.c`，而在用户代码区插入：

```c
/* USER CODE Includes */
#include "board_config.h"
#include "esp8266_http.h"
#include "dht11.h"
#include "mq135.h"
#include "pir.h"
#include "sensor_frame.h"

/* main 初始化末尾 */
app_setup();

/* while(1) 内 */
app_loop_once();
```

把 `firmware/Src/main.c` 里 `app_setup` / `app_loop_once` / `app_sample` 复制到单独 `app_task.c`，避免与 CubeMX 双 main。

## 4. 编译宏

C/C++ → Define 可加：

```
STM32F103xB,USE_HAL_DRIVER
```

与 CubeMX 生成一致即可。
