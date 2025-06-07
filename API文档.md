
* * * * *

**EasyRide 后端 API 文档**
======================

版本: 1.0

最后更新: 2025年6月7日

**1\. 引言**
----------

本文档详细描述了 EasyRide 平台后端微服务的所有API接口。所有客户端（iOS App, Web管理后台）应通过API网关与这些服务进行通信。

### **1.1. 通用设计原则**

-   **统一响应格式**: 所有API成功时返回 `{"code": 0, "message": "Success", "data": {}}` 结构。失败时，`code` 为非零错误码，`message` 包含错误信息，`data` 为 `null`。
-   **认证 (Authentication)**: 大多数需要用户登录的接口，必须在HTTP请求的 `Authorization` 头中携带 `Bearer <JWT>` 令牌。
-   **安全签名 (Security Signature)**: 涉及到资金、创建或修改关键数据的POST/PUT/DELETE请求，必须在HTTP头中包含以下字段以防止重放攻击和参数篡改：
    -   `X-Timestamp`: 请求发出的Unix时间戳（秒）。
    -   `X-Nonce`: 一次性的随机字符串。
    -   `X-Signature`: 基于 `timestamp`、`nonce` 和请求体（或参数）使用共享密钥计算出的HMAC-SHA256签名。
-   **幂等性 (Idempotency)**: 所有创建或修改资源的`POST`请求，建议客户端生成一个唯一的 `Idempotency-Key` 放入请求头，以防止因网络重试导致重复创建。

* * * * *

**2\. 用户服务 (User Service)**
---------------------------

**服务地址前缀**: `/api/users`

该服务负责用户注册、登录、认证和资料管理。

### **2.1. 认证接口 (Authentication)**

#### `POST /register`

-   **功能**: 注册新用户（乘客或司机）。
-   **描述**: 使用 `multipart/form-data` 以支持司机注册时上传证件。
-   **请求 (`multipart/form-data`)**:
    -   `registrationDto` (部分, `application/json`): 包含用户基本信息的JSON对象。

        JSON

        ```
        {
          "username": "new_driver_01",
          "password": "a_strong_password",
          "email": "driver01@example.com",
          "phoneNumber": "+14155552671",
          "otpCode": "123456",
          "role": "DRIVER",
          "driverLicenseNumber": "D12345678"
        }

        ```

    -   `driverLicenseFile` (部分, file, 可选): 驾驶证扫描件/照片。
    -   `vehicleDocumentFile` (部分, file, 可选): 车辆登记文件照片。
-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "注册成功",
      "data": {
        "id": 101,
        "username": "new_driver_01",
        "email": "driver01@example.com",
        "phoneNumber": "+14155552671",
        "role": "DRIVER"
      }
    }

    ```

#### `POST /login`

-   **功能**: 使用用户名/手机号和密码登录。
-   **请求体**: `LoginRequest`

    JSON

    ```
    {
      "username": "new_driver_01",
      "password": "a_strong_password"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "登录成功",
      "data": {
        "accessToken": "eyJhbGciOiJIUzI1Ni...",
        "tokenType": "Bearer"
      }
    }

    ```

#### `POST /otp/request-login`

-   **功能**: 请求发送用于登录的短信验证码。
-   **请求体**: `RequestOtpDto`

    JSON

    ```
    {
      "phoneNumber": "+14155552671"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "OTP已发送至您的手机",
      "data": null
    }

    ```

#### `POST /login/otp`

-   **功能**: 使用手机号和验证码进行登录。
-   **请求体**: `PhoneOtpLoginRequestDto`

    JSON

    ```
    {
      "phoneNumber": "+14155552671",
      "otpCode": "123456"
    }

    ```

-   **成功响应 (200 OK)**: `data` 字段包含 `JwtAuthenticationResponse` 对象。

### **2.2. 密码管理 (Password Management)**

#### `POST /password/request-otp`

-   **功能**: 为忘记密码流程请求验证码。
-   **请求体**: `ForgotPasswordRequestDto`

    JSON

    ```
    {
      "identifier": "+14155552671" // 可以是手机号或邮箱
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "如果账户存在，OTP已发送至关联的手机号/邮箱",
      "data": null
    }

    ```

#### `POST /password/reset-otp`

-   **功能**: 使用验证码重置密码。
-   **请求体**: `ResetPasswordRequestDto`

    JSON

    ```
    {
      "identifier": "+14155552671",
      "otpCode": "123456",
      "newPassword": "my_new_secure_password"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "密码重置成功",
      "data": null
    }

    ```

### **2.3. 个人资料接口 (Profile API)**

#### `GET /profile`

-   **功能**: 获取当前登录用户的个人资料。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // UserProfileDto
        "id": 101,
        "username": "new_driver_01",
        "email": "driver01@example.com",
        "phoneNumber": "+14155552671",
        "role": "DRIVER",
        "approvalStatus": "PENDING_REVIEW", // 仅司机有
        "vehicleInfo": "Toyota Camry 2021", // 仅司机有
        "commonAddresses": [ // 仅乘客有
            { "alias": "Home", "address": "123 Main St" }
        ]
      }
    }

    ```

#### `PUT /profile`

-   **功能**: 更新当前登录用户的个人资料。
-   **请求体**: `UserProfileUpdateDto`

    JSON

    ```
    {
      "email": "driver01.updated@example.com",
      "vehicleInfo": "Honda CR-V 2022"
    }

    ```

-   **成功响应 (200 OK)**: `data` 字段包含更新后的 `UserProfileDto`。

* * * * *

**3\. 订单服务 (Order Service)**
----------------------------

**服务地址前缀**: `/api/orders`

负责订单的整个生命周期管理，从创建到完成。

### **3.1. 订单核心流程 (Core Order Flow)**

#### `POST /create`

-   **功能**: 乘客创建一个新的出行订单。
-   **描述**: 订单创建后，状态为 `PENDING_MATCH`（待匹配）或 `SCHEDULED`（已预约），并发布一个 `ORDER_CREATED` 事件到消息队列，由匹配服务进行处理。
-   **请求体**: `OrderCreateDto`

    JSON

    ```
    {
      "passengerId": 202,
      "startLocation": { "latitude": 34.0522, "longitude": -118.2437 },
      "endLocation": { "latitude": 34.1522, "longitude": -118.3437 },
      "vehicleType": "STANDARD",
      "serviceType": "NORMAL",
      "paymentMethodId": 501,
      "scheduledTime": null, // null表示立即出行
      "passengerNotes": "行李较多，请注意"
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "订单创建请求已提交，等待匹配司机",
      "data": { // OrderResponseDto
        "orderId": 1002,
        "status": "PENDING_MATCH",
        "estimatedCost": 28.50,
        "estimatedDistance": 12.5,
        "estimatedDuration": 20
      }
    }

    ```

#### `GET /{orderId}`

-   **功能**: 获取指定订单的详细信息。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // OrderResponseDto
        "orderId": 1002,
        "status": "DRIVER_ASSIGNED",
        "passengerName": "Alice",
        "driverName": "Bob",
        "driverRating": 4.8,
        "vehicleInfo": "Toyota Prius - ABC 123",
        "estimatedCost": 28.50,
        // ...更多订单详情
      }
    }

    ```

#### `POST /{orderId}/cancel`

-   **功能**: 乘客或司机（在特定条件下）取消订单。
-   **描述**: 系统会根据预设规则（如取消时间、司机是否已出发）计算可能产生的取消费用。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "订单已取消",
      "data": {
        "cancellationFee": 5.00 // 可能会有取消费用
      }
    }

    ```

### **3.2. 行程中接口 (In-Trip API)**

#### `POST /{orderId}/status`

-   **功能**: 司机更新订单的实时状态。
-   **描述**: 司机通过此接口通知系统已到达上车点 (`ARRIVED`)、接到乘客 (`IN_PROGRESS`) 或完成行程 (`COMPLETED`)。
-   **请求体**: `UpdateOrderStatusDto`

    JSON

    ```
    {
      "status": "ARRIVED"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "订单状态已更新",
      "data": null
    }

    ```

#### `POST /{orderId}/location`

-   **功能**: 司机端高频上报行程中的实时位置。
-   **请求体**: `TripLocationUpdateDto`

    JSON

    ```
    {
      "latitude": 34.0888,
      "longitude": -118.2888
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "位置更新成功",
      "data": null
    }

    ```

#### `GET /{orderId}/driver-location`

-   **功能**: 乘客端获取行程中司机的实时位置。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // LocationDto
        "latitude": 34.0888,
        "longitude": -118.2888
      }
    }

    ```

* * * * *

**4\. 匹配服务 (Matching Service)**
-------------------------------

**服务地址前缀**: `/api/matching`

内部服务，主要通过消息队列与订单服务交互。也提供部分API供司机端进行手动操作。

#### `GET /orders/available`

-   **功能**: 司机获取当前可供抢单的订单列表。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": [ // List<AvailableOrderDto>
        {
          "orderId": 1003,
          "startAddress": "洛杉矶国际机场",
          "endAddress": "加州大学洛杉矶分校",
          "serviceType": "AIRPORT_TRANSFER",
          "estimatedFare": 65.00,
          "distanceToPickupKm": 3.1
        }
      ]
    }

    ```

#### `POST /orders/{orderId}/accept`

-   **功能**: 司机接受（抢）一个在`available`列表中的订单。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "订单接受成功",
      "data": null
    }

    ```

#### `POST /driverStatus/{driverId}`

-   **功能**: 司机更新其工作状态（上线/下线）和当前位置。
-   **请求体**: `DriverStatusUpdateDto`

    JSON

    ```
    {
      "available": true,
      "currentLatitude": 34.0522,
      "currentLongitude": -118.2437
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "司机状态更新成功",
      "data": null
    }

    ```

* * * * *

**5\. 支付服务 (Payment Service)**
------------------------------

**服务地址前缀**: `/api/payments`

负责处理支付、退款、司机钱包和提现。

### **5.1. 乘客支付方式管理**

#### `POST /methods`

-   **功能**: 乘客添加新的支付方式。
-   **描述**: 客户端通过支付网关SDK（如Stripe.js）获取一次性`nonce`，然后将其发送到此接口。
-   **请求体**: `AddPaymentMethodRequestDto`

    JSON

    ```
    {
      "methodType": "CREDIT_CARD",
      "paymentGatewayNonce": "pm_1Jabcde..."
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "支付方式添加成功",
      "data": { // PaymentMethodResponseDto
        "id": 502,
        "methodType": "CREDIT_CARD",
        "displayName": "Visa ending in 4242",
        "isDefault": true
      }
    }

    ```

#### `GET /methods`

-   **功能**: 获取当前登录乘客的所有已保存支付方式。
-   **成功响应 (200 OK)**: `data` 字段为 `List<PaymentMethodResponseDto>`。

#### `DELETE /methods/{paymentMethodId}`

-   **功能**: 删除一个已保存的支付方式。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "支付方式删除成功",
      "data": null
    }

    ```

### **5.2. 司机钱包与提现**

#### `GET /wallet`

-   **功能**: 获取当前登录司机的钱包信息。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // WalletDto
        "balance": 1250.75,
        "currency": "USD"
      }
    }

    ```

#### `POST /withdrawals/request`

-   **功能**: 司机发起提现申请。
-   **请求体**: `WithdrawalRequestDto`

    JSON

    ```
    {
      "amount": 200.00,
      "payoutMethodId": 601 // 司机预先绑定的银行账户ID
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "提现申请已提交，等待处理",
      "data": { // WithdrawalResponseDto
        "withdrawalId": "w_12345",
        "status": "PENDING"
      }
    }

    ```

* * * * *

**6\. 其他服务API**
---------------------

以下服务主要通过内部消息队列进行通信，但它们也提供关键的API接口，供客户端、其他微服务或管理后台直接调用。

### **6.1. 评价服务 (Review Service)**

**服务地址前缀**: `/api`

负责处理用户对行程和司机的评价、评分、标签以及相关的投诉与申诉流程。

#### `POST /evaluations`

-   **功能**: 用户（乘客或司机）提交对已完成行程的评价。
-   **描述**: 系统会校验评价窗口是否开启（例如，行程结束后7天内）。提交的评价内容会经过敏感词过滤。
-   **认证**: 需要用户JWT。
-   **请求体**: `EvaluationDTO`

    JSON

    ```
    {
      "orderId": 1002,
      "evaluatorId": 202, // 评价者ID (来自JWT)
      "evaluateeId": 101, // 被评价者ID
      "score": 5, // 评分 (1-5)
      "comment": "司机师傅非常专业，车内环境干净整洁。",
      "tags": ["服务态度好", "车内整洁"] // 预设或自定义的标签
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "评价提交成功",
      "data": { // 返回创建的评价详情
        "id": 77,
        "orderId": 1002,
        "score": 5,
        "comment": "司机师傅非常专业，车内环境干净整洁。"
      }
    }

    ```

#### `GET /evaluations/evaluatee/{userId}`

-   **功能**: 获取指定用户（通常是司机）收到的所有评价。
-   **描述**: 用于在司机个人资料页展示其历史评分和评价。会进行分页处理。
-   **认证**: 需要用户JWT。
-   **URL参数**:
    -   `userId` (long): 被查询用户的ID。
-   **查询参数**:
    -   `page` (int, 可选, 默认0): 页码。
    -   `size` (int, 可选, 默认10): 每页数量。
-   **成功响应 (200 OK)**: `data` 字段为包含 `EvaluationDTO` 的分页对象。

#### `POST /complaints`

-   **功能**: 用户针对某次行程或评价提交投诉。
-   **描述**: 允许用户提交文字理由，并可上传图片等证据文件。
-   **认证**: 需要用户JWT。
-   **请求 (`multipart/form-data`)**:
    -   `complaintDto` (部分, `application/json`):

        JSON

        ```
        {
          "evaluationId": 77, // 关联的评价ID，可选
          "orderId": 1002,    // 关联的订单ID
          "complainantId": 202, // 投诉人ID (来自JWT)
          "reason": "司机绕路导致费用远超预估。"
        }

        ```

    -   `evidenceFiles` (部分, file array, 可选): 证据文件（如截图、录音）。
-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "投诉已提交，平台将尽快处理",
      "data": {
        "complaintId": 301,
        "status": "PENDING_REVIEW"
      }
    }

    ```

### **6.2. 位置服务 (Location Service)**

**服务地址前缀**: `/api`

提供地理位置相关的核心功能，包括地址解析、地理围栏和安全监控。

#### `GET /location/info`

-   **功能**: 反向地理编码。
-   **描述**: 根据经纬度坐标返回对应的地理位置信息（如格式化地址）。
-   **认证**: 需要客户端或服务凭证。
-   **查询参数**:
    -   `lat` (double, 必须): 纬度。
    -   `lon` (double, 必须): 经度。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // LocationResponse from Google Maps API etc.
        "formattedAddress": "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA",
        "city": "Mountain View",
        "zipCode": "94043"
      }
    }

    ```

#### `GET /geofences/check`

-   **功能**: 检查一个坐标点是否在特定类型的地理围栏内。
-   **描述**: 用于判断订单起点/终点是否在服务区内，或司机是否进入了特殊计费区域/禁行区。
-   **认证**: 需要服务凭证。
-   **查询参数**:
    -   `lat` (double, 必须): 纬度。
    -   `lon` (double, 必须): 经度。
    -   `type` (string, 可选): 围栏类型过滤，如 `SERVICE_AREA`。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": [ // 返回所有包含该点的围栏信息
        {
          "id": 1,
          "name": "主服务区",
          "type": "SERVICE_AREA"
        }
      ]
    }

    ```

### **6.3. 通知服务 (Notification Service)**

该服务主要由内部事件驱动，但可提供一个测试接口。

#### `POST /notifications/send-test`

-   **功能**: 发送一则测试通知。
-   **描述**: 仅供管理员或开发人员测试通知渠道（Push, SMS, Email）是否正常工作。
-   **认证**: 需要管理员JWT。
-   **请求体**:

    JSON

    ```
    {
      "channel": "SMS", // "SMS", "EMAIL", "PUSH_APNS", "PUSH_FCM"
      "recipient": "+14155552671", // 手机号、邮箱或设备token
      "title": "测试标题", // PUSH/EMAIL需要
      "body": "这是一条来自EasyRide的测试消息。"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "测试通知已发送",
      "data": null
    }

    ```

### **6.4. 数据分析服务 (Analytics Service)**

**服务地址前缀**: `/api/analytics`

为管理后台提供数据支持。

#### `POST /query`

-   **功能**: 查询聚合后的分析指标数据。
-   **描述**: 用于为仪表盘、报表提供数据。支持按时间、维度进行分组和聚合。
-   **认证**: 需要管理员JWT。
-   **请求体**: `AnalyticsQueryDto`

    JSON

    ```
    {
      "metricName": "order_revenue_total",
      "startDate": "2025-06-01T00:00:00",
      "endDate": "2025-06-06T23:59:59",
      "granularity": "DAILY", // "HOURLY", "DAILY", "MONTHLY"
      "filters": {
        "serviceType": "AIRPORT_TRANSFER"
      }
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // AnalyticsResponseDto
        "metricName": "order_revenue_total",
        "totalValue": 15678.50,
        "chartData": [
          { "label": "2025-06-01", "value": 2345.00 },
          { "label": "2025-06-02", "value": 2500.50 }
          // ... more data points
        ]
      }
    }

    ```

### **6.5. 管理后台服务 (Admin Service)**

**服务地址前缀**: `/api/admin`

提供平台运营管理所需的所有接口。

#### `GET /platform-users`

-   **功能**: 分页、筛选查询平台所有用户。
-   **认证**: 需要管理员JWT (`USER_SUPPORT_MANAGER` 或更高权限)。
-   **查询参数**:
    -   `page`, `size`, `role`, `searchTerm`。
-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { // UserPageDto
        "content": [ /* UserDetailDto 列表 */ ],
        "totalPages": 10,
        "totalElements": 98,
        "currentPage": 0
      }
    }

    ```

#### `POST /platform-users/{userId}/disable`

-   **功能**: 禁用（封禁）一个用户账户。
-   **认证**: 需要管理员JWT (`USER_SUPPORT_MANAGER` 或更高权限)。
-   **请求体**:

    JSON

    ```
    {
      "reason": "违反平台规定"
    }

    ```

-   **成功响应 (200 OK)**: `data` 为 `null`。

#### `GET /drivers/applications/pending`

-   **功能**: 获取所有待审核的司机申请列表。
-   **认证**: 需要管理员JWT (`DRIVER_VERIFICATION` 或更高权限)。
-   **成功响应 (200 OK)**: `data` 字段为 `DriverApplicationDto` 的分页对象。

#### `POST /drivers/applications/{driverId}/approve`

-   **功能**: 批准一个司机的入驻申请。
-   **认证**: 需要管理员JWT (`DRIVER_VERIFICATION` 或更高权限)。
-   **请求体**:

    JSON

    ```
    {
      "notes": "所有证件审核通过。"
    }

    ```

-   **成功响应 (200 OK)**: `message` 为 "司机申请已批准"。

#### `POST /orders/intervene`

-   **功能**: 管理员手动干预一个异常订单。
-   **认证**: 需要管理员JWT (`ORDER_OPERATOR` 或更高权限)。
-   **请求体**: `AdminOrderInterveneEvent`

    JSON

    ```
    {
      "orderId": 1005,
      "action": "REASSIGN", // "REASSIGN", "CANCEL"
      "reason": "原司机车辆故障，重新指派。"
    }

    ```

-   **成功响应 (200 OK)**: `message` 为 "订单干预指令已发送"。

#### `GET /finance/withdrawals`

-   **功能**: 查看待处理的司机提现申请。
-   **认证**: 需要管理员JWT (`FINANCE_MANAGER` 或更高权限)。
-   **成功响应 (200 OK)**: `data` 为 `WithdrawalRequestDto` 的分页对象。

#### `POST /finance/withdrawals/{withdrawalId}/process`

-   **功能**: 处理一笔提现申请（批准或拒绝）。
-   **认证**: 需要管理员JWT (`FINANCE_MANAGER` 或更高权限)。
-   **请求体**:

    JSON

    ```
    {
      "action": "APPROVE", // "APPROVE", "REJECT"
      "notes": "财务已审核"
    }

    ```

-   **成功响应 (200 OK)**: `message` 为 "提现请求已处理"。

#### `PUT /system-config/pricing-rules`

-   **功能**: 更新平台的计价规则。
-   **认证**: 需要管理员JWT (`SYSTEM_CONFIG_ADMIN` 或更高权限)。
-   **请求体**:

    JSON

    ```
    {
      "baseFare": 3.00,
      "perKmRate": 1.60,
      "perMinuteRate": 0.25,
      "airportSurcharge": 5.50
    }

    ```

-   **成功响应 (200 OK)**: `message` 为 "计价规则更新成功"。

* * * * *
