**User Service**
----------------
概述
--

**用户服务** (`user_service`) 是一个独立的微服务，负责管理系统中的用户相关操作。它提供了乘客和司机的注册、认证和资料管理功能，确保用户数据的安全存储、高效管理以及与其他服务的无缝通信。

* * * * *

功能特点
----

-   **用户注册**：支持乘客和司机两种用户角色的注册功能。
-   **用户资料管理**：允许更新用户名、电子邮箱和角色等信息。
-   **用户信息查询**：提供获取用户详细信息的接口。
-   **事件发布**：将用户相关的操作事件发布到 Rocketmq，供其他服务使用。
-   **安全 API**：通过 JWT 身份验证保护用户数据。

* * * * *

架构设计
----

`user_service` 遵循分层架构设计，确保职责分离，易于维护：

1.  **模型层**：定义用户数据结构。
2.  **数据传输对象 (DTO)**：用于在层之间或与外部客户端交换数据。
3.  **持久层 (Repository)**：使用 Spring Data JPA 管理数据的存储和查询。
4.  **服务层**：封装用户相关的业务逻辑。
5.  **控制层**：提供 RESTful API 接口。
6.  **Rocketmq 集成**：实现与其他服务的异步通信。

* * * * *

技术栈
---

-   **编程语言**：Java 17
-   **框架**：Spring Boot 3.1.0
-   **数据库**：MySQL
-   **消息队列**：rocketmq
-   **安全**：JWT (JSON Web Tokens)
-   **依赖管理**：Maven
-   **工具**：Lombok (用于简化代码)

* * * * *

API 文档
------

### 创建用户

-   **URL**：`POST /users/create`
-   **描述**：注册一个新用户（乘客或司机）。
-   **请求体**：



    `{
      "username": "john_doe",
      "email": "john@example.com",
      "role": "PASSENGER"
    }`

-   **响应**：



    `{
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "role": "PASSENGER"
    }`

### 更新用户

-   **URL**：`PUT /users/{userId}`
-   **描述**：更新用户信息。
-   **请求体**：



    `{
      "username": "john_doe_updated",
      "email": "john_updated@example.com",
      "role": "DRIVER"
    }`

-   **响应**：



    `{
      "id": 1,
      "username": "john_doe_updated",
      "email": "john_updated@example.com",
      "role": "DRIVER"
    }`

