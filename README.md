Analytics Service
-----------------

概述
--

"数据分析服务"(analytics_service) 是一个独立的微服务，用于收集并分析平台的运营数据。它通过 RocketMQ 与其他服务（如 order_service、user_service、payment_service）进行通信，将增量数据写入数据库并提供报表与可视化查询功能。该服务为平台运营人员提供数据支撑，帮助他们进行数据驱动的决策和改进。

功能特点
----

1.  **增量数据采集**：监听来自各微服务（order-topic、user-topic 等）的事件，实时写入数据库。
2.  **运营指标管理**：支持订单量、客单价、活跃用户、司机接单率等多种关键指标的采集与查询。
3.  **数据查询与可视化**：通过 RESTful API 提供多维度的数据分析接口，并可生成图表与报表。
4.  **预测与决策支持**：基于历史数据预测订单趋势及需求峰值，为市场及调度决策提供依据。
5.  **隐私保护**：在存储前对含敏感信息的字段进行脱敏或匿名化，保证合规与安全。

架构设计
----

analytics_service 采用模块化架构，易于扩展与维护：

- 模型层（Model）：定义 AnalyticsRecord 等实体，映射数据库表存储运营数据。\
- 数据传输对象（DTO）：如 OrderCompletedEvent、AnalyticsRequestDto，服务间或前端后端间传递数据。\
- 持久层（Repository）：使用 Spring Data JPA（AnalyticsRepository 等）管理数据库访问。\
- 服务层（Service）：处理核心分析与聚合逻辑（AnalyticsServiceImpl 等）。\
- 控制层（Controller）：提供 /analytics/record、/analytics/query 等接口，处理数据存储和查询请求。\
- RocketMQ 集成：使用 @RocketMQMessageListener 监听各服务 Topic，将增量事件转为 AnalyticsRecord 写入数据库。\
- 隐私与安全：通过 PrivacyUtil 在写入数据库前进行字段脱敏，防止敏感信息泄露。

技术栈
---

- 编程语言：Java 17\
- 框架：Spring Boot 3.1.0\
- 数据库：MySQL\
- 消息队列：RocketMQ\
- 依赖管理：Maven\
- 工具：Lombok（简化实体和 DTO 编写）

API 文档
------

写入分析数据（POST /analytics/record）\
说明：接收外部或管理后台的增量数据，用于写入数据库。

示例请求体：

```
{ 
"recordType": "ORDER_DATA", 
"metricName": "orderRevenue", 
"metricValue": 99.99, 
"recordTime": "2023-09-22T10:30:00", 
"dimensionKey": "region", 
"dimensionValue": "East" 
}
```

查询分析结果（POST /analytics/query）\
说明：根据时间段、指标名称、维度等条件进行聚合计算，返回可视化数据或统计值。

示例请求体：

```
{ 
"metricName": "orderRevenue", 
"recordType": "ORDER_DATA", 
"startTime": "2023-09-01T00:00:00", 
"endTime": "2023-09-30T23:59:59", 
"dimensionKey": "region", 
"dimensionValue": "East" 
}
```

导出数据报表（POST /analytics/report）\
说明：根据查询条件生成报表数据，可返回给前端制作 PDF/Excel。与 /analytics/query 类似，示例略。

通过以上功能，analytics_service 能够实现对平台运营数据的实时收集与分析，为决策提供数据支撑。
