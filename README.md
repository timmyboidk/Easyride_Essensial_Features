* * * * *

EasyRide 微服务项目 README
=====================

1\. 项目概述
--------

**EasyRide** 是一个采用微服务架构构建的出行平台，专注于为用户提供高效、安全、便捷的出行服务。系统由一系列分工明确、可独立部署和扩展的微服务组成，通过 **RocketMQ** 消息队列进行异步通信和解耦，确保了系统的高可用性和弹性。

2\. 核心微服务模块
-----------

整个平台由以下几个核心微服务构成，每个服务都承担着明确的业务职责：

### 2.1. 用户服务 (user_service)

-   **核心职责**: 作为所有用户（乘客和司机）的统一管理中心，负责用户的整个生命周期。

-   **主要功能**:

    -   **用户注册与认证**: 支持乘客和司机的注册，并通过 JWT (JSON Web Tokens) 提供安全的身份验证。

    -   **资料管理**: 允许用户查看和更新个人信息。

    -   **事件发布**: 将用户相关的关键操作（如注册、资料更新）作为事件发布到 RocketMQ，供其他服务消费。

### 2.2. 订单服务 (order_service)

-   **核心职责**: 管理订单的完整生命周期，从创建、分配到最终完成。

-   **主要功能**:

    -   **订单创建**: 接收来自乘客的订单请求，并进行费用和时间的初步预估。

    -   **状态管理**: 精确追踪并管理订单的各种状态流转（如待接单、行程中、已完成）。

    -   **司机分配**: 根据距离、车型等条件匹配最合适的司机。

    -   **事件驱动**: 通过 RocketMQ 与用户、支付、匹配等服务进行高效的异步通信。

### 2.3. 支付服务 (payment_service)

-   **核心职责**: 统一处理平台的所有支付相关业务，包括乘客支付、司机钱包管理和提现功能。

-   **主要功能**:

    -   **支付处理**: 支持多种支付渠道，处理乘客的支付请求。

    -   **钱包管理**: 为司机提供应用内钱包，记录收入明细。

    -   **提现功能**: 允许司机将钱包余额提现到绑定的银行账户。

    -   **安全保障**: 采用应用层加密、Redis防抖、MD5签名和防篡改措施，确保交易的安全性与幂等性。

### 2.4. 匹配服务 (matching_service)

-   **核心职责**: 作为订单分配的智能大脑，根据乘客的订单需求和司机的实时状态，高效地匹配最合适的司机。

-   **主要功能**:

    -   **自动匹配**: 通过距离、评分、车型等多维度综合计算，为订单智能推荐最优司机。

    -   **手动接单与抢单**: 支持司机在手动模式下浏览并选择订单，或参与抢单模式。

    -   **状态同步**: 通过消息队列与 `order_service` 和 `user_service` 实时同步订单和司机状态。

### 2.5. 位置服务 (location_service)

-   **核心职责**: 提供所有与地理位置相关的功能。

-   **主要功能**:

    -   **实时位置追踪**: 实时获取并更新司机和乘客的位置信息。

    -   **位置查询与地图服务**: 集成地图服务提供商（如 Google Maps），实现路径规划、距离计算等功能。

    -   **地理围栏**: 支持设置服务区域、特殊计费区或禁行区。

### 2.6. 通知服务 (notification_service)

-   **核心职责**: 负责处理平台所有对外的通讯通知。

-   **主要功能**:

    -   **事件驱动**: 监听由其他微服务（如订单、支付、用户服务）发布的业务事件。

    -   **多渠道分发**: 根据事件类型和用户偏好，通过短信（SMS）、邮件（Email）和移动推送（APNs for iOS, FCM for Android）等渠道发送通知。

    -   **模板化内容**: 使用模板引擎生成动态、本地化的通知内容。

### 2.7. 评价服务 (review_service)

-   **核心职责**: 管理用户（乘客与司机）之间的双向评价体系。

-   **主要功能**:

    -   **评价与评分**: 允许乘客和司机在行程结束后相互评分和文字评价。

    -   **投诉与申诉**: 提供流程让用户可以对不满意的服务进行投诉，并允许被投诉方进行申诉。

    -   **内容审核**: 包含敏感词过滤机制，确保平台内容的合规性。

### 2.8. 数据分析服务 (analytics_service)

-   **核心职责**: 作为平台的数据中枢，负责收集、分析所有运营数据，为业务决策提供数据支持。

-   **主要功能**:

    -   **数据采集**: 通过监听各微服务发布的业务事件，实时采集增量数据。

    -   **指标管理与报表**: 支持对订单量、活跃用户、交易额等多维度关键指标的分析，并生成报表。

    -   **数据可视化**: 提供数据查询接口，为管理后台的数据可视化提供支持。

    -   **隐私保护**: 在数据存储前对敏感信息进行脱敏处理，保障用户数据安全。

### 2.9. 管理后台服务 (admin_service)

-   **核心职责**: 为平台运营人员提供一个统一的管理与监控后台系统。

-   **主要功能**:

    -   **用户管理**: 查看和审核所有用户信息，处理违规账户。

    -   **订单监控与干预**: 实时监控订单状态，并在必要时进行手动干预，如重新分配或取消订单。

    -   **财务管理**: 查看平台收入流水，管理司机提现申请。

    -   **系统配置**: 管理平台的各项规则，如价格、抽成、通知模板等。

3\. 技术栈
-----------
**后端框架**

Spring Boot 3.x

构建微服务的基础框架

**编程语言**

Java 17

主要开发语言

**数据持久化**

Spring Data JPA, MySQL

关系型数据存储

**消息队列**

RocketMQ

微服务间的异步通信与解耦

**安全认证**

Spring Security, JWT

API接口的安全与用户身份验证

**依赖管理**

Maven

项目构建和依赖管理

**开发工具**

Lombok

简化Java代码编写

**支付网关**

PayPal SDK, Stripe SDK

处理信用卡等在线支付

4. Project Structure & Build
---------------------------

This project is organized as a **Maven Multi-Module** project.

### 4.1. Directory Structure

```
├── pom.xml                 # Root Parent POM (manages dependencies & modules)
├── infrastructure/         # Centralized Infrastructure Configuration
│   ├── docker-compose.yml  # Orchestrates MySQL, Redis, RocketMQ, Kafka
│   ├── mysql/init/         # Database initialization scripts
│   └── rocketmq/conf/      # Broker configuration
├── admin_service/          # Module: Admin Dashboard Backend
├── analytics_service/      # Module: Data Analytics Service
├── location_service/       # Module: Location & Maps Service
├── matching_service/       # Module: Order Matching Engine
├── notification_service/   # Module: Notification Dispatcher
├── order_service/          # Module: Order Management Service
├── payment_service/        # Module: Payment & Wallet Service
├── review_service/         # Module: Ratings & Reviews Service
└── user_service/           # Module: User Identity & Profile Service
```

### 4.2. Build Instructions

To build the entire project (all services) at once:

```bash
mvn clean install
```

### 4.3. Infrastructure Setup

We provide a centralized Docker Compose configuration to spin up all required middleware (MySQL, Redis, RocketMQ, Kafka).

```bash
cd infrastructure
docker-compose up -d
```

**Services Started:**
- **MySQL**: Port 3306 (Databases auto-created)
- **Redis**: Port 6379
- **RocketMQ NameServer**: Port 9876
- **RocketMQ Broker**: Port 10911
- **Zookeeper**: Port 2181
- **Kafka**: Port 9092

### 4.4. Running Services

Each service contains a `Dockerfile`. You can build individual images or run them locally using `mvn spring-boot:run`.
