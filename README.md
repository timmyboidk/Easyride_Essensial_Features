# EasyRide 微服务

**EasyRide** 是一个基于 **Spring Boot** 微服务构建的云原生网约车平台，专为高可扩展性和可靠性而设计。

## 快速入门

### 1. 环境要求
- Java 17
- Maven 3.8+
- Docker & Docker Compose

### 2. 基础设施配置
启动 MySQL、Redis、RocketMQ 和 Kafka：
```bash
cd infrastructure
docker-compose up -d
```

### 3. 构建与运行
构建所有服务：
```bash
mvn clean install
```
运行特定服务：
```bash
cd user_service
mvn spring-boot:run
```

## 文档

完整文档位于 `docs/` 目录：

- **[架构设计](docs/ARCH_DESIGN.md)**：系统概述、消息队列与数据库设计。
- **[API 参考](docs/API_REFERENCE.md)**：REST API 规范。
- **[安装指南](docs/SETUP.md)**：详细安装与环境配置。
- **[开发者指南](docs/DEV_GUIDE.md)**：代码规范、测试与贡献指南。
- **[运维与部署](docs/DEVOPS.md)**：Docker、CI/CD管道及生产环境部署。
- **[测试清单](docs/TESTING.md)**：单元测试与集成测试计划。

## 项目结构

```
EasyRide/
├── pom.xml                 # 根父级POM文件
├── infrastructure/         # Docker Compose及配置文件
├── docs/                   # 文档目录
├── user_service/           # 身份认证与用户资料
├── order_service/          # 订单管理
├── payment_service/        # 钱包与支付
├── matching_service/       # 司机匹配引擎
├── location_service/       # 地理位置追踪
├── notification_service/   # 短信/邮件/推送通知
├── review_service/         # 评分系统
├── analytics_service/      # 数据与报告
└── admin_service/          # 后台管理面板
```

## 技术栈
- **框架**：Spring Boot 3.4.1
- **数据库**：MySQL 8.0
- **缓存**：Redis
- **消息系统**：Apache RocketMQ 5.x
- **构建**：Maven多模块
- **容器**：Docker

---
&copy; 2026 EasyRide团队
