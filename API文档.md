EasyRide 后端 API 文档 (终版)
=======================

**版本**: 1.1

1\. 引言
------

### 1.1. 通用设计原则

-   **统一响应格式**: 所有API在成功时返回一个标准的JSON结构。

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { ... }
    }

    ```

    失败时，`code` 为非零错误码，`message` 包含错误信息，`data` 为 `null`。

-   **认证 (Authentication)**: 大多数需要用户登录的接口，必须在HTTP请求的 `Authorization` 头中携带 `Bearer <JWT>` 令牌。

-   **安全签名 (Security Signature)**: 涉及到资金、创建或修改关键数据的 `POST`/`PUT`/`DELETE` 请求，必须在HTTP头中包含以下字段以防止重放攻击和参数篡改：

    -   `X-Timestamp`: 请求发出的Unix时间戳（秒）。

    -   `X-Nonce`: 一次性的随机字符串。

    -   `X-Signature`: 基于 `timestamp`、`nonce` 和请求体（或参数）使用共享密钥计算出的HMAC-SHA256签名。

-   **幂等性 (Idempotency)**: 所有创建或修改资源的 `POST` 请求，建议客户端生成一个唯一的 `Idempotency-Key` 放入请求头，以防止因网络重试导致重复创建。

* * * * *

2\. 用户服务 (user_service)
-----------------------

**基础路径**: `/api/user`

### 2.1. 认证与注册 (`/api/user/auth`)

#### `POST /register`

-   **功能**: 注册新用户（乘客或司机）。

-   **请求体**:

    JSON

    ```
    {
      "phoneNumber": "13912345678",
      "password": "StrongPassword123!",
      "role": "PASSENGER"
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "User registered successfully",
      "data": {
        "userId": 201,
        "phoneNumber": "13912345678",
        "role": "PASSENGER",
        "accessToken": "ey..._a_jwt_token_...dA"
      }
    }

    ```

#### `POST /login/password`

-   **功能**: 使用手机号和密码登录。

-   **请求体**:

    JSON

    ```
    {
      "username": "13912345678",
      "password": "StrongPassword123!"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Login successful",
      "data": {
        "accessToken": "ey..._a_new_jwt_token_...fG"
      }
    }

    ```

#### `POST /login/otp`

-   **功能**: 使用手机号和验证码登录。

-   **请求体**:

    JSON

    ```
    {
      "phoneNumber": "13912345678",
      "otpCode": "654321"
    }

    ```

-   **成功响应 (200 OK)**: (响应结构同上)

#### `POST /otp/request`

-   **功能**: 请求发送短信验证码。

-   **请求体**:

    JSON

    ```
    {
      "phoneNumber": "13912345678"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "OTP sent successfully.",
      "data": null
    }

    ```

#### `POST /password/reset`

-   **功能**: 重置密码。

-   **认证**: 需要有效的JWT。

-   **请求体**:

    JSON

    ```
    {
      "oldPassword": "StrongPassword123!",
      "newPassword": "AnotherStrongPassword456!"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Password has been reset successfully.",
      "data": null
    }

    ```

### 2.2. 用户资料管理 (`/api/user/profile`)

#### `GET /`

-   **功能**: 获取当前登录用户的个人资料。

-   **认证**: 需要有效的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "userId": 201,
        "phoneNumber": "13912345678",
        "nickname": "Rider One",
        "avatarUrl": "https://example.com/avatars/201.jpg",
        "role": "PASSENGER",
        "status": "ACTIVE"
      }
    }

    ```

#### `PUT /`

-   **功能**: 更新当前登录用户的个人资料。

-   **认证**: 需要有效的JWT。

-   **请求体**:

    JSON

    ```
    {
      "nickname": "Rider VIP",
      "avatarUrl": "https://example.com/avatars/201_new.jpg"
    }

    ```

-   **成功响应 (200 OK)**: (响应结构同上, 但包含更新后的数据)

### 2.3. 司机专属功能

#### `POST /driver/register`

-   **功能**: 司机提交入驻申请。

-   **认证**: 需要有效的JWT。

-   **请求体**:

    JSON

    ```
    {
      "realName": "张三",
      "idCardNumber": "310101199001011234",
      "driverLicenseNumber": "D12345678",
      "carModel": "Tesla Model Y",
      "carLicensePlate": "沪A88888",
      "attachments": {
        "idCardFrontUrl": "https://s3.bucket/id_front.jpg",
        "idCardBackUrl": "https://s3.bucket/id_back.jpg",
        "driverLicenseUrl": "https://s3.bucket/license.jpg",
        "carInsuranceUrl": "https://s3.bucket/insurance.jpg"
      }
    }

    ```

-   **成功响应 (202 Accepted)**:

    JSON

    ```
    {
      "code": 0,
      "message": "您的申请已提交，正在审核中。",
      "data": {
        "applicationId": 55,
        "verificationStatus": "PENDING"
      }
    }

    ```

* * * * *

3\. 订单服务 (order_service)
------------------------

**基础路径**: `/api/order`

#### `POST /`

-   **功能**: 乘客创建一个新的出行订单。

-   **认证**: 需要乘客角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "serviceType": "LONG_DISTANCE",
      "pickupLocation": { "latitude": 31.2304, "longitude": 121.4737, "address": "人民广场" },
      "dropoffLocation": { "latitude": 31.1443, "longitude": 121.8083, "address": "浦东国际机场T2" },
      "pickupTime": "2025-07-15T10:30:00Z",
      "passengerCount": 2
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Order created successfully",
      "data": {
        "orderId": 1002,
        "status": "PENDING_MATCH",
        "estimatedFare": 250.00
      }
    }

    ```

#### `GET /{orderId}`

-   **功能**: 获取特定订单的详细信息。

-   **认证**: 需要订单相关的乘客或司机的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "orderId": 1002,
        "passengerId": 201,
        "driverId": 501,
        "status": "IN_PROGRESS",
        "serviceType": "LONG_DISTANCE",
        "pickupLocation": { "address": "人民广场" },
        "dropoffLocation": { "address": "浦东国际机场T2" },
        "estimatedFare": 250.00,
        "driverInfo": {
          "realName": "李四",
          "carModel": "BYD Han",
          "carLicensePlate": "沪B99999",
          "serviceRatingAvg": 4.9
        }
      }
    }

    ```

#### `PUT /{orderId}/status`

-   **功能**: 更新订单状态。

-   **认证**: 需要司机角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "status": "DRIVER_ARRIVED"
    }

    ```

-   **成功响应 (200 OK)**: (响应结构同上, 但`status`已更新)

#### `POST /{orderId}/cancel`

-   **功能**: 取消一个订单。

-   **认证**: 需要订单相关的乘客或司机的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "订单已取消",
      "data": {
        "orderId": 1002,
        "status": "CANCELLED",
        "cancellationFee": 5.00
      }
    }

    ```

#### `GET /history`

-   **功能**: 获取当前用户的历史订单列表。

-   **认证**: 需要有效的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "content": [
          { "orderId": 1002, "status": "COMPLETED", "actualFare": 255.00, "dropoffLocation": {"address": "浦东国际机场T2"}, "createdTime": "2025-07-15T10:30:00Z"}
        ],
        "totalPages": 1,
        "totalElements": 1
      }
    }

    ```

#### `POST /estimate-price`

-   **功能**: 预估订单价格。

-   **认证**: 需要有效的JWT。

-   **请求体**: (结构同创建订单，但可只含必要字段)

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "estimatedFare": 250.00,
        "distance": "55 km",
        "estimatedDuration": "60 mins"
      }
    }

    ```

* * * * *

4\. 支付服务 (payment_service)
--------------------------

**基础路径**: `/api/payment`

### 4.1. 支付处理 (`/api/payment/payments`)

#### `POST /`

-   **功能**: 为指定订单发起支付。

-   **认证**: 需要乘客角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "orderId": 1002,
      "paymentMethodId": "pm_1L9pZg2eZvKYlo2C8c6t3XnZ"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Payment successful",
      "data": {
        "paymentId": "pi_3L9pZk2eZvKYlo2C1gXqJq9N",
        "status": "SUCCESS",
        "amount": 255.00
      }
    }

    ```

### 4.2. 钱包管理 (`/api/payment/wallet`)

#### `GET /`

-   **功能**: 获取当前用户的钱包信息（司机）。

-   **认证**: 需要司机角色的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "driverId": 501,
        "balance": 850.75,
        "currency": "USD"
      }
    }

    ```

#### `POST /withdrawals`

-   **功能**: 司机发起提现请求。

-   **认证**: 需要司机角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "amount": 500.00,
      "withdrawalMethodId": "wth_paypal_account_123"
    }

    ```

-   **成功响应 (202 Accepted)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Withdrawal request submitted",
      "data": {
        "withdrawalId": "wd_abc12345",
        "amount": 500.00,
        "status": "PENDING_REVIEW"
      }
    }

    ```

### 4.3. 支付方式管理 (`/api/payment/methods`)

#### `POST /`

-   **功能**: 乘客添加新的支付方式。

-   **认证**: 需要乘客角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "type": "CREDIT_CARD",
      "token": "tok_1L9pZg2eZvKYlo2C..."
    }

    ```

-   **成功响应 (201 Created)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "paymentMethodId": "pm_1L9pZg2eZvKYlo2C8c6t3XnZ",
        "type": "CREDIT_CARD",
        "details": "VISA **** 4242"
      }
    }

    ```

#### `GET /`

-   **功能**: 获取乘客已绑定的支付方式列表。

-   **认证**: 需要乘客角色的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": [
        { "paymentMethodId": "pm_1L9pZg2eZvKYlo2C8c6t3XnZ", "type": "CREDIT_CARD", "details": "VISA **** 4242", "isDefault": true }
      ]
    }

    ```

#### `DELETE /{methodId}`

-   **功能**: 删除一个已绑定的支付方式。

-   **认证**: 需要乘客角色的JWT。

-   **成功响应 (204 No Content)**

* * * * *

5\. 匹配服务 (matching_service)
---------------------------

**基础路径**: `/api/matching`

#### `POST /driver/status`

-   **功能**: 司机更新自己的状态。

-   **认证**: 需要司机角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "status": "ONLINE"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Status updated",
      "data": {
        "driverId": 501,
        "status": "ONLINE"
      }
    }

    ```

#### `GET /driver/orders`

-   **功能**: 司机获取附近可接的订单列表。

-   **认证**: 需要司机角色的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": [
        {
          "orderId": 1003,
          "serviceType": "AIRPORT_PICKUP",
          "estimatedFare": 180.00,
          "pickupDistance": "2.5 km",
          "tripDistance": "30 km",
          "pickupLocation": { "address": "静安寺" },
          "dropoffLocation": { "address": "虹桥机场T1" }
        }
      ]
    }

    ```

#### `POST /driver/grab`

-   **功能**: 司机抢单。

-   **认证**: 需要司机角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "orderId": 1003
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "抢单成功",
      "data": {
        "orderId": 1003,
        "matchStatus": "SUCCESS"
      }
    }

    ```

* * * * *

6\. 位置服务 (location_service)
---------------------------

**基础路径**: `/api/location`

#### `POST /driver/update`

-   **功能**: 司机实时上传自己的位置信息。

-   **认证**: 需要司机角色的JWT。

-   **请求体**:

    JSON

    ```
    {
      "latitude": 31.2222,
      "longitude": 121.4581,
      "timestamp": 1678886400
    }

    ```

-   **成功响应 (200 OK)**: (无响应体)

#### `GET /order/{orderId}`

-   **功能**: 乘客获取进行中订单的司机实时位置。

-   **认证**: 需要乘客角色的JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "latitude": 31.2222,
        "longitude": 121.4581,
        "etaToPickup": "5 mins"
      }
    }

    ```

### 6.1. 地理围栏管理 (`/api/location/geofences`)

#### `POST /`

-   **功能**: (管理员) 创建地理围栏。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "name": "Pudong Airport Area",
      "type": "AIRPORT",
      "shape": {
        "type": "Polygon",
        "coordinates": [[[...],[...],[...]]]
      }
    }

    ```

-   **成功响应 (201 Created)**: (返回创建的 Geofence DTO)

#### `GET /`

-   **功能**: (管理员) 获取所有地理围栏。

-   **认证**: 需要管理员JWT。

-   **成功响应 (200 OK)**: (返回 Geofence DTO 列表)

* * * * *

7\. 评价服务 (review_service)
-------------------------

**基础路径**: `/api/review`

#### `POST /`

-   **功能**: 提交一条评价。

-   **认证**: 需要有效的JWT。

-   **请求体**:

    JSON

    ```
    {
      "orderId": 1002,
      "rating": 5,
      "comment": "司机师傅服务态度很好，车内干净整洁！",
      "tags": ["态度超棒", "车内整洁"]
    }

    ```

-   **成功响应 (201 Created)**: (返回创建的评价DTO)

#### `GET /order/{orderId}`

-   **功能**: 查看某个订单的评价。

-   **认证**: 需要有效的JWT。

-   **成功响应 (200 OK)**: (返回评价DTO列表)

#### `POST /complaints`

-   **功能**: 提交一条投诉。

-   **认证**: 需要有效的JWT。

-   **请求体**:

    JSON

    ```
    {
      "orderId": 1004,
      "reason": "危险驾驶",
      "description": "司机在高速上多次急刹车，感觉非常不安全。",
      "attachmentUrls": ["https://s3.bucket/video_proof.mp4"]
    }

    ```

-   **成功响应 (201 Created)**: (返回创建的投诉DTO)

* * * * *

8\. 数据分析服务 (analytics_service)
------------------------------

**基础路径**: `/api/analytics`

#### `GET /dashboard/summary`

-   **功能**: (管理员) 获取运营仪表盘的摘要数据。

-   **认证**: 需要管理员JWT。

-   **成功响应 (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": {
        "totalOrdersToday": 1500,
        "totalGmvToday": 75000.50,
        "onlineDrivers": 350,
        "activeUsers": 5000
      }
    }

    ```

#### `POST /reports/generate`

-   **功能**: (管理员) 生成指定类型的报表。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "reportType": "WEEKLY_GMV",
      "startDate": "2025-07-01",
      "endDate": "2025-07-07"
    }

    ```

-   **成功响应 (200 OK)**: (返回图表或时序数据点列表)

* * * * *

9\. 管理后台服务 (admin_service)
--------------------------

**基础路径**: `/api/admin`

### 9.1. 用户管理 (`/api/admin/users`)

#### `GET /`

-   **功能**: 分页查询用户列表。

-   **认证**: 需要管理员JWT。

-   **成功响应 (200 OK)**: (返回用户分页列表)

#### `PUT /{userId}/status`

-   **功能**: 更改用户状态。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "status": "INACTIVE",
      "reason": "违反平台规定"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "用户状态已更新", "data": null }

    ```

### 9.2. 司机管理 (`/api/admin/drivers`)

#### `GET /applications`

-   **功能**: 查看待审核的司机入驻申请。

-   **认证**: 需要管理员JWT。

-   **成功响应 (200 OK)**: (返回司机申请的分页列表)

#### `POST /applications/{driverId}/approve`

-   **功能**: 批准司机的入驻申请。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "notes": "所有证件审核通过。"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "司机申请已批准", "data": null }

    ```

### 9.3. 订单与财务

#### `POST /orders/intervene`

-   **功能**: 管理员手动干预异常订单。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "orderId": 1005,
      "action": "FORCE_CANCEL",
      "reason": "乘客紧急情况，免责取消。"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "订单干预指令已发送", "data": null }

    ```

#### `GET /finance/withdrawals`

-   **功能**: 查看待处理的司机提现申请。

-   **认证**: 需要管理员JWT。

-   **成功响应 (200 OK)**: (返回提现申请的分页列表)

#### `POST /finance/withdrawals/{withdrawalId}/process`

-   **功能**: 处理一笔提现申请。

-   **认证**: 需要管理员JWT。

-   **请求体**:

    JSON

    ```
    {
      "action": "APPROVE",
      "notes": "已打款，交易号 T123456789"
    }

    ```

-   **成功响应 (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "提现请求已处理", "data": null }

    ```
