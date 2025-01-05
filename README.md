**Analytics Service**
-----------------

概述
--

**数据分析服务** ( `analytics_service` ) 是一个独立的微服务，用于收集并分析平台的运营数据。它通过 **RocketMQ** 订阅其他服务（如 `order_service`, `user_service`, `payment_service`）发送的事件，将增量数据写入数据库并提供报表与可视化查询功能。该服务帮助平台运营人员进行数据驱动的决策，改进服务质量与用户体验。

* * * * *

功能特点
----

- **增量数据采集**：监听 `order-topic`、`user-topic` 等，实时获取订单完成、用户注册、司机评分等信息。
- **运营指标管理**：存储订单量、客单价、活跃用户、司机接单率等多种关键运营指标。
- **数据查询与可视化**：提供多维度、多指标的查询接口；可生成图表与报表。
- **预测与决策支持**：基于历史数据预测订单趋势，为营销活动提供依据。
- **隐私保护**：在写入数据库前对敏感字段进行脱敏或匿名化，确保合规与安全。

* * * * *

架构设计
----

`analytics_service` 采用模块化架构，确保系统可扩展、易维护：

1. **模型层 (Model)**：定义 `AnalyticsRecord` 等实体，映射运营数据表。
2. **数据传输对象 (DTO)**：如 `OrderCompletedEvent`、`AnalyticsRequestDto`，在服务间或与客户端传递数据。
3. **持久层 (Repository)**：使用 Spring Data JPA 管理数据的存储和查询 (`AnalyticsRepository` 等)。
4. **服务层 (Service)**：实现核心分析与聚合逻辑（`AnalyticsServiceImpl` 等）。
5. **控制层 (Controller)**：提供 RESTful API 用于录入、查询、导出报表等。
6. **RocketMQ 集成**：监听 `order-topic`、`user-topic` 等，实现对增量事件的实时消费与处理。
7. **隐私与安全**：通过 `PrivacyUtil` 进行敏感数据脱敏，保护个人隐私。

* * * * *

技术栈
---

- **编程语言**：Java 17
- **框架**：Spring Boot 3.1.0
- **数据库**：MySQL（或可扩展至数据仓库）
- **消息队列**：RocketMQ
- **依赖管理**：Maven
- **工具**：Lombok (简化代码)

* * * * *

API 文档
------

### 写入分析数据

- **URL**：`POST /analytics/record`
- **描述**：接收来自其他服务或管理后台的增量数据，写入数据库。
- **请求体**（示例）：

  ```json
  {
    "recordType": "ORDER_DATA",
    "metricName": "orderRevenue",
    "metricValue": 99.99,
    "recordTime": "2023-09-22T10:30:00",
    "dimensionKey": "region",
    "dimensionValue": "East"
  }

-   **响应**：无

### 查询分析结果

-   **URL**：`POST /analytics/query`
-   **描述**：根据指标名称、时间范围、维度等查询聚合数据或明细。
-   **请求体**（示例）：

  ```json
{
  "metricName": "orderRevenue",
  "recordType": "ORDER_DATA",
  "startTime": "2023-09-01T00:00:00",
  "endTime": "2023-09-30T23:59:59",
  "dimensionKey": "region",
  "dimensionValue": "East"
}

-   **响应**（示例）：

  ```json
{
  "metricName": "orderRevenue",
  "recordType": "ORDER_DATA",
  "dimensionKey": "region",
  "dimensionValue": "East",
  "totalValue": 12345.67,
  "chartData": [
    {
      "label": "2023-09-10T10:30:00",
      "value": 99.99
    },
    {
      "label": "2023-09-11T15:20:00",
      "value": 200.0
    }
  ]
}


-   **URL**：`POST /analytics/report`
-   **描述**：根据查询条件生成报表，可扩展为导出PDF/Excel。
-   **请求体**：与 `/analytics/query` 类似
-   **响应**（示例）：

  ```json
{
  "reportTitle": "数据报表：orderRevenue",
  "chartData": [
    { "label": "2023-09-10T10:30:00", "value": 99.99 },
    { "label": "2023-09-11T15:20:00", "value": 200.0 }
  ]
}

