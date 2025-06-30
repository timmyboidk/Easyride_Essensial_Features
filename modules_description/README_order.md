**Order Service**
-----------------

概述
--

**订单服务** (`order_service`) 是一个独立的微服务，负责管理与订单相关的操作，包括订单创建、分配司机、状态更新和取消等功能。它与用户服务和支付服务通过 rocketmq 消息队列进行通信，以保证数据的同步和流程的高效执行。

* * * * *

功能特点
----

-   **订单创建**：接收乘客发起的订单请求，分配司机并预估费用和行程时间。
-   **订单状态管理**：支持订单状态的流转，如待接单、已接单、行程中等。
-   **司机分配**：通过距离、车型等条件自动匹配最优司机。
-   **取消订单**：允许在不同状态下取消订单。
-   **与用户/支付服务通信**：通过 rocketmq 进行事件通知。

* * * * *

架构设计
----

`order_service` 采用模块化架构，确保系统的可扩展性和易维护性：

1.  **模型层**：定义订单、乘客和司机的结构。
2.  **数据传输对象 (DTO)**：在服务间或与客户端之间传递数据。
3.  **持久层 (Repository)**：使用 Spring Data JPA 管理数据的存储和查询。
4.  **服务层**：实现核心业务逻辑，如订单分配、状态更新等。
5.  **控制层**：提供 RESTful API 接口。
6.  **Rocketmq 集成**：用于异步消息的发送和接收。

* * * * *

技术栈
---

-   **编程语言**：Java 17
-   **框架**：Spring Boot 3.1.0
-   **数据库**：MySQL
-   **消息队列**：rocketmq
-   **依赖管理**：Maven
-   **工具**：Lombok (用于简化代码)

* * * * *

API 文档
------

### 创建订单

-   **URL**：`POST /orders/create`
-   **描述**：创建一个新的订单。
-   **请求体**：


    `{
      "passengerId": 1,
      "startLocation": {
        "latitude": 30.0,
        "longitude": 120.0
      },
      "endLocation": {
        "latitude": 31.0,
        "longitude": 121.0
      },
      "vehicleType": "STANDARD",
      "serviceType": "NORMAL",
      "paymentMethod": "CREDIT_CARD"
    }`

-   **响应**：



    `{
      "orderId": 100,
      "status": "PENDING",
      "passengerName": "Test Passenger",
      "driverName": "Test Driver",
      "estimatedCost": 100.0,
      "estimatedDistance": 200.0,
      "estimatedDuration": 60.0
    }`

### 获取订单详情

-   **URL**：`GET /orders/{orderId}`
-   **描述**：获取特定订单的详细信息。
-   **响应**：



    `{
      "orderId": 100,
      "status": "PENDING",
      "passengerName": "Test Passenger",
      "driverName": "Test Driver",
      "estimatedCost": 100.0,
      "estimatedDistance": 200.0,
      "estimatedDuration": 60.0
    }`
