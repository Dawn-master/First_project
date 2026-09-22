# JavaWeb 后端（Spring Boot）

多传感器环境监测系统服务端：接收硬件上报、入库、阈值告警、舒适度评分，供微信小程序调用。

## 技术栈

- Java 21 + Spring Boot 3.4（Java Web）
- Spring Web + Spring Data JPA
- H2（开发）/ MySQL（生产）
- IDEA 直接打开本 `backend/` 目录即可

## 数据库

默认 **H2 内存库**，无需安装；配置在 `src/main/resources/application.yml`。

- 表：`sensor_reading`（传感器读数）、`alert_record`（告警）
- 控制台：`http://127.0.0.1:8080/h2-console`（URL `jdbc:h2:mem:envmonitor`，用户 `sa`，密码空）
- 切换 MySQL：执行 `sql/schema.sql` 建库，再把 profile 切到 `prod` 并改账号密码

详细说明见 **[sql/README.md](sql/README.md)**。

## 启动

```powershell
# 命令行
$mvn = "C:\Users\$env:USERNAME\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd"
cd backend
& $mvn spring-boot:run "-Dspring-boot.run.arguments=--env.monitor.simulate-enabled=true"
```

或 IDEA 运行 `EnvMonitorApplication`。

地址：`http://127.0.0.1:8080`  
默认设备：`sensor-001`  
设备 Key：`X-Device-Key: env-monitor-2026`

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/device/data` | 硬件上报 |
| GET | `/api/sensor/latest` | 最新数据 |
| GET | `/api/sensor/history?hours=24` | 历史 |
| GET | `/api/sensor/score` | 舒适度 |
| GET | `/api/sensor/dashboard` | 小程序聚合面板 |
| GET | `/api/alerts` | 告警 |
| POST | `/api/alerts/{id}/resolve` | 处理告警 |
| POST | `/api/simulate/start\|stop` | 模拟数据 |

## 硬件上报 JSON

```json
{
  "deviceId": "sensor-001",
  "temperature": 25.3,
  "humidity": 58.0,
  "pir": 1,
  "mq135": 132.5
}
```
