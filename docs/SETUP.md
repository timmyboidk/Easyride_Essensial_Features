# EasyRide 配置与安装

## 1. 环境要求

请确保您的开发环境满足以下要求：
- **Java**：JDK 17 或更高版本
- **Maven**：3.8+
- **Docker & Docker Compose**：已安装并运行
- **Git**：用于版本控制

## 2. 基础架构配置
我们使用 Docker Compose 启动所有必要的中间件服务（MySQL、Redis、RocketMQ、Kafka）。

1. 进入基础设施目录：
   ```bash
   cd infrastructure
   ```

2. 启动服务：
   ```bash
   docker-compose up -d
   ```

3. 验证服务运行状态：
   ```bash
   docker ps
   ```
   应可见 `easyride-mysql`、`easyride-redis`、`rmqnamesrv`、`rmqbroker`、`easyride-zookeeper` 及 `easyride-kafka` 容器。

## 3. 数据库初始化
`docker-compose.yml`通过挂载`./mysql/init`实现首次运行时自动初始化MySQL数据库模式。

如需手动重置数据库：
1. 停止容器：`docker-compose down -v`（将删除卷！）
2. 重启：`docker-compose up -d`

## 4. 应用配置
每个服务均配有 `application.yml`（或 `properties`）文件。默认配置为连接 Docker 映射的 `localhost` 端口。

**关键环境变量**（可选覆盖）：
- `SPRING_DATASOURCE_HOST`：数据库主机（默认：localhost）
- `SPRING_REDIS_HOST`：Redis主机（默认：localhost）
- `ROCKETMQ_NAME_SERVER`：RocketMQ地址（默认：localhost:9876）

## 5. 运行应用程序

### 方案A：命令行运行
可通过Maven单独启动任意服务：

```bash
# 运行用户服务
cd user_service
mvn spring-boot:run
```

### 方案 B：在 IDE 中运行（IntelliJ IDEA / Eclipse）
1. 以项目形式打开根目录的 `pom.xml`。
2. IDE 将自动检测全部 9 个模块。
3. 导航至目标服务的应用程序主类（例如 `UserServiceApplication.java`）。
4. 右键点击并选择**运行**。

## 6. 验证
服务运行后可测试健康检查接口：
- 管理服务：`http://localhost:8080/actuator/health`
- 用户服务：`http://localhost:8081/actuator/health`
...依此类推。
