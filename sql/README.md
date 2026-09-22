# 数据库与后端如何连接

## 当前已接通：MySQL 8.3（本机）

| 项 | 值 |
|----|-----|
| 版本 | MySQL **8.3.0** |
| 端口 | **3307** |
| 用户 / 密码 | `root` / `1103110` |
| 数据库 | `env_monitor` |
| 表 | `sensor_reading`、`alert_record` |
| 配置 profile | **`prod`**（`application.yml` 默认） |
| JDBC | `jdbc:mysql://127.0.0.1:3307/env_monitor` |

磁盘位置（默认）：

`C:\ProgramData\MySQL\MySQL Server 8.3\Data\env_monitor\`

## 本机另一套 MySQL（备用）

| 服务 | 端口 | 密码 | profile |
|------|------|------|---------|
| MySQL 5.5.37 | 3306 | `148627` | `mysql55` |

## 连接方式

硬件/小程序 **不直连数据库**，只访问后端 HTTP；后端通过 JDBC 写库。

```
固件/串口桥/小程序 → http://127.0.0.1:8080 → Spring Data JPA → MySQL 8.3
```

配置在 `backend/src/main/resources/application.yml` 的 `prod` 段。

## 验证数据

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.3\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -p1103110 -e "USE env_monitor; SELECT COUNT(*) FROM sensor_reading; SELECT * FROM sensor_reading ORDER BY id DESC LIMIT 5;"
```

Navicat/DataGrip：`127.0.0.1:3307`，`root` / `1103110`，库 `env_monitor`。

## profile 对照

| profile | 数据源 |
|---------|--------|
| **`prod`（默认）** | MySQL 8.3 @3307 / `env_monitor` |
| `mysql55` | MySQL 5.5 @3306 |
| `dev` | H2 内存（不落盘） |
| `h2file` | 项目目录文件 H2 |

切换示例：

```powershell
java -jar backend\target\env-monitor-backend-1.0.0.jar --spring.profiles.active=prod
```
