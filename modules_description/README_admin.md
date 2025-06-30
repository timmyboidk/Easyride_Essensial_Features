Admin Service
-------------

概述
--

"管理后台服务"(admin_service) 是一个独立的微服务，供平台运营人员使用的管理与监控系统。其主要功能包括用户管理、订单监控和干预、财务管理以及系统配置等。该服务通过 RocketMQ 与 order_service、user_service、payment_service 等进行通信，确保业务流程的及时反馈与手动干预。

功能特点
----

1.  **用户管理**：查看、审核乘客和司机的信息、订单历史、评价记录；处理违规用户或黑名单。
2.  **订单管理**：实时监控订单状态，需要时手动重新分配或取消订单；查看投诉与异常订单。
3.  **财务管理**：可查看平台收入、司机提现情况、调整抽成比例和活动优惠等。
4.  **系统配置**：管理通知模板、推送策略、APP 版本更新、公告等；设定匹配算法参数。
5.  **权限管理**：为不同运营人员分配角色与权限，并记录审计日志追踪后台操作行为。

架构设计
----

admin_service 采用分层和模块化设计，便于扩展和维护：

- 模型层（Model）：定义 AdminUser 实体与角色枚举 (Role)。\
- DTO（数据传输对象）：如 AdminUserDto, OrderInterveneDto，用于与前端交互或服务间传输数据。\
- 持久层（Repository）：使用 Spring Data JPA（AdminUserRepository）管理管理员用户等表的增删改查。\
- 服务层（Service）：实现后台业务逻辑，如创建管理员用户、禁用用户、手动干预订单等 (AdminServiceImpl)。\
- 控制层（Controller）：提供 RESTful API，如 /admin/users, /admin/orders/intervene 等。\
- RocketMQ 通信：可监听或发送消息，与其他微服务配合处理订单和财务事件。

技术栈
---

- 编程语言：Java 17\
- 框架：Spring Boot 3.1.0\
- 数据库：MySQL\
- 消息队列：RocketMQ\
- 依赖管理：Maven\
- 工具：Lombok (简化实体和 DTO)

API 文档
------

创建管理员用户（POST /admin/users）\
说明：创建新的后台管理员账号。\
示例请求体：

```
{ "username": "adminA", "password": "pass123", "role": "FINANCE", "enabled": true }
```

更新管理员用户（PUT /admin/users）\
说明：更新管理员账号信息，如角色、密码。\
示例请求体：

```
{ "id": 1, "username": "adminB", "password": "securePass", "role": "SUPER_ADMIN", "enabled": false }
```

禁用管理员用户（POST /admin/users/{adminUserId}/disable）\
说明：禁用指定管理员账号。返回 HTTP 200 OK。

手动干预订单（POST /admin/orders/intervene）\
说明：重新分配或取消订单，用于处理特殊异常。\
示例请求体：
```
{ "orderId": 12345, "action": "REASSIGN", "reason": "司机无故离线" }
```
* * * * *

admin_service 可扩展更多功能，如黑名单管理、APP 公告配置、匹配算法全局设置等，为平台运营人员提供全方位的后台管理能力。
