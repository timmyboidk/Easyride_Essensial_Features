Matching Service
----------------

概述
--

匹配服务 (matching_service) 是一个独立的微服务，负责根据乘客订单信息与司机当前状态，自动或手动分配最合适的司机，以保证订单得到及时、合理的处理。该服务可通过 RocketMQ 与 order_service、user_service 等进行通信，实现订单创建事件订阅与司机状态信息的同步。

功能特点
----

1.  自动匹配：通过综合计算司机与乘客的距离、评分、车型等维度，为订单选出最优司机。
2.  司机手动接单：支持司机在手动模式下浏览待接订单并选择是否接受或拒绝。
3.  抢单模式：发布新订单到抢单队列，供附近司机自主抢单；成功后更新订单状态。
4.  动态司机状态：记录司机的实时位置、可用性、评分，用于更精准的匹配计算。
5.  网络延迟与并发控制：使用分布式锁或回滚机制，防止重复分配或竞争接单。

架构设计
----

matching_service 采用模块化设计，主要包括：

1.  模型层 (Model)：定义 DriverStatus, MatchingRecord 等实体，映射至数据库，存储司机位置、评分以及匹配历史。
2.  数据传输对象 (DTO)：如 MatchRequestDto, DriverStatusDto, OrderCreatedEvent，用于服务间或前端后端间数据交换。
3.  持久层 (Repository)：使用 Spring Data JPA (DriverStatusRepository 等) 对司机状态、匹配记录执行增删改查。
4.  服务层 (Service)：核心匹配逻辑与业务方法 (MatchingServiceImpl)，如 matchDriver()、updateDriverStatus()。
5.  控制层 (Controller)：提供 /matching/matchDriver、/matching/driverStatus 等 REST 接口供外部调用。
6.  RocketMQ 集成：监听 order-topic 的新订单事件 (OrderEventListener)，并向 order_service 或 user_service 发送分配结果。

技术栈
---

-   编程语言：Java 17
-   框架：Spring Boot 3.1.0
-   数据库：MySQL
-   消息队列：RocketMQ
-   依赖管理：Maven
-   工具：Lombok (用于简化实体和 DTO)

API 文档
------

1.  自动匹配 (POST /matching/matchDriver) 说明：根据订单需求（下单位置、车型等）返回最优司机ID；若无司机可用则返回 null。\
    示例请求体：
```
    { "orderId": 101, "passengerId": 10, "startLatitude": 30.0, "startLongitude": 120.0, "vehicleType": "STANDARD", "serviceType": "NORMAL", "paymentMethod": "CREDIT_CARD", "estimatedCost": 99.99 }
```

2.  更新司机状态 (POST /matching/driverStatus/{driverId}) 说明：司机上报位置信息和可用状态。\
    示例请求参数：
```
    /driverStatus/201?latitude=30.1&longitude=120.2&available=true&rating=4.5&vehicleType=STANDARD
```
3.  接受订单 (POST /matching/acceptOrder) 说明：司机点击"接受订单"，通知 matching_service，后续可回调 order_service。\
    示例：
```
    /acceptOrder?orderId=101&driverId=201
```

通过以上功能，matching_service 能够高效地完成订单与司机之间的匹配流程，支持自动与手动多种模式，并确保与外部微服务的消息交互畅通。
