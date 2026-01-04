# EasyRide 后端 API 文档

**版本**: 1.1

1. 简介

---

### 1.1. 通用设计原则

* **统一响应格式**：所有 API 在成功时返回标准的 JSON 结构。
JSON
```
{
  "code": 0,
  "message": "Success",
  "data": { ... }
}


```


失败时，`code` 为非零错误码，`message` 包含错误信息，`data` 为 `null`。
* **身份验证**：对于大多数需要用户登录的接口，必须在 HTTP 请求的 `Authorization` 头中包含 `Bearer <JWT>` 令牌。
* **安全签名**：对于涉及资金、创建或修改关键数据的 `POST`/`PUT`/`DELETE` 请求，必须在 HTTP 头中包含以下字段，以防止重放攻击和参数篡改：
* `X-Timestamp`：Unix 时间戳（秒），即请求发送的时间。
* `X-Nonce`：一个一次性随机字符串。
* `X-Signature`：使用 `timestamp`、`nonce` 和请求体（或参数）与共享密钥计算出的 HMAC-SHA256 签名。


* **幂等性**：对于所有创建或修改资源的 `POST` 请求，建议客户端在请求头中生成唯一的 `Idempotency-Key`，以防止因网络重试导致重复创建。

---

2. 用户服务 (user_service)

---

**基础路径**: `/api/user`

### 2.1. 身份验证与注册 (`/api/user/auth`)

#### `POST /register`

* **功能**：注册新用户（乘客或司机）。
* **请求体**：
JSON
```
{
  "phoneNumber": "13912345678",
  "password": "StrongPassword123!",
  "role": "PASSENGER"
}


```


* **成功响应 (201 Created)**：
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

* **功能**：使用手机号和密码登录。
* **请求体**：
JSON
```
{
  "username": "13912345678",
  "password": "StrongPassword123!"
}


```


* **成功响应 (200 OK)**：
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

* **功能**：使用手机号和验证码登录。
* **请求体**：
JSON
```
{
  "phoneNumber": "13912345678",
  "otpCode": "654321"
}


```


* **成功响应 (200 OK)**：（响应结构同上）

#### `POST /otp/request`

* **功能**：请求短信验证码。
* **请求体**：
JSON
```
{
  "phoneNumber": "13912345678"
}


```


* **成功响应 (200 OK)**：
JSON
```
{
  "code": 0,
  "message": "OTP sent successfully.",
  "data": null
}


```



#### `POST /password/reset`

* **功能**：重置密码。
* **身份验证**：需要有效的 JWT。
* **请求体**：
JSON
```
{
  "oldPassword": "StrongPassword123!",
  "newPassword": "AnotherStrongPassword456!"
}


```


* **成功响应 (200 OK)**：
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

* **功能**：获取当前登录用户的资料。
* **身份验证**：需要有效的 JWT。
* **成功响应 (200 OK)**：
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

* **功能**：更新当前登录用户的资料。
* **身份验证**：需要有效的 JWT。
* **请求体**：
JSON
```
{
  "nickname": "Rider VIP",
  "avatarUrl": "https://example.com/avatars/201_new.jpg"
}


```


* **成功响应 (200 OK)**：（响应结构同上，但包含更新后的数据）

### 2.3. 司机专属功能

#### `POST /driver/register`

* **功能**：提交司机入驻申请。
* **身份验证**：需要有效的 JWT。
* **请求体**：
JSON
```
{
  "realName": "Zhang San",
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


* **成功响应 (202 Accepted)**：
JSON
```
{
  "code": 0,
  "message": "Your application has been submitted and is under review.",
  "data": {
    "applicationId": 55,
    "verificationStatus": "PENDING"
  }
}


```



---

3. 订单服务 (order_service)

---

**基础路径**: `/api/order`

#### `POST /`

* **功能**：乘客创建新的行程订单。
* **身份验证**：需要具有乘客角色的 JWT。
* **请求体**：
JSON
```
{
  "serviceType": "LONG_DISTANCE",
  "pickupLocation": { "latitude": 31.2304, "longitude": 121.4737, "address": "People's Square" },
  "dropoffLocation": { "latitude": 31.1443, "longitude": 121.8083, "address": "Pudong International Airport T2" },
  "pickupTime": "2025-07-15T10:30:00Z",
  "passengerCount": 2
}


```


* **成功响应 (201 Created)**：
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

* **功能**：获取特定订单的详细信息。
* **身份验证**：需要与该订单关联的乘客或司机的 JWT。
* **成功响应 (200 OK)**：
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
    "pickupLocation": { "address": "People's Square" },
    "dropoffLocation": { "address": "Pudong International Airport T2" },
    "estimatedFare": 250.00,
    "driverInfo": {
      "realName": "Li Si",
      "carModel": "BYD Han",
      "carLicensePlate": "沪B99999",
      "serviceRatingAvg": 4.9
    }
  }
}


```



#### `PUT /{orderId}/status`

* **功能**：更新订单状态。
* **身份验证**：需要具有司机角色的 JWT。
* **请求体**：
JSON
```
{
  "status": "DRIVER_ARRIVED"
}


```


* **成功响应 (200 OK)**：（响应结构同上，但 `status` 已更新）

#### `POST /{orderId}/cancel`

* **功能**：取消订单。
* **身份验证**：需要与该订单关联的乘客或司机的 JWT。
* **成功响应 (200 OK)**：
JSON
```
{
  "code": 0,
  "message": "Order cancelled",
  "data": {
    "orderId": 1002,
    "status": "CANCELLED",
    "cancellationFee": 5.00
  }
}


```



#### `GET /history`

* **功能**：获取当前用户的历史订单列表。
* **身份验证**：需要有效的 JWT。
* **成功响应 (200 OK)**：
JSON
```
{
  "code": 0,
  "message": "Success",
  "data": {
    "content": [
      { "orderId": 1002, "status": "COMPLETED", "actualFare": 255.00, "dropoffLocation": {"address": "Pudong International Airport T2"}, "createdTime": "2025-07-15T10:30:00Z"}
    ],
    "totalPages": 1,
    "totalElements": 1
  }
}


```



#### `POST /estimate-price`

* **功能**：预估订单价格。
* **身份验证**：需要有效的 JWT。
* **请求体**：（结构与创建订单相同，但仅需必要字段）
* **成功响应 (200 OK)**：
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



---

4. 支付服务 (payment_service)

---

**基础路径**: `/api/payment`

### 4.1. 支付处理 (`/api/payment/payments`)

#### `POST /`

* **功能**：为指定订单发起支付。
* **身份验证**：需要具有乘客角色的 JWT。
* **请求体**：
JSON
```
{
  "orderId": 1002,
  "paymentMethodId": "pm_1L9pZg2eZvKYlo2C8c6t3XnZ"
}


```


* **成功响应 (200 OK)**：
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

* **功能**：获取当前用户的钱包信息（司机）。
* **身份验证**：需要具有司机角色的 JWT。
* **成功响应 (200 OK)**：
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


#### **`GET /transactions` **


* **功能**：获取当前认证司机的钱包交易历史（分页）。
* **身份验证**：需要具有司机角色的 JWT。
* **查询参数**：
* `page` (int, 可选, 默认 0): 页码。
* `size` (int, 可选, 默认 20): 每页大小。


* **成功响应 (200 OK)**：
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "content": [
      {
        "transaction_id": 101,
        "type": "INCOME",
        "amount": 55.50,
        "related_order_id": 1002,
        "status": "COMPLETED",
        "transaction_date": "2025-07-15T12:30:00Z"
      },
      {
        "transaction_id": 102,
        "type": "WITHDRAWAL",
        "amount": -500.00,
        "related_withdrawal_id": 45,
        "status": "COMPLETED",
        "transaction_date": "2025-07-14T10:00:00Z"
      }
    ],
    "totalPages": 5,
    "totalElements": 98
  }
}

```



#### `POST /withdrawals`

* **功能**：司机发起提现请求。
* **身份验证**：需要具有司机角色的 JWT。
* **请求体**：
JSON
```
{
  "amount": 500.00,
  "withdrawalMethodId": "wth_paypal_account_123"
}


```


* **成功响应 (202 Accepted)**：
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

* **功能**：乘客添加新的支付方式。
* **身份验证**：需要具有乘客角色的 JWT。
* **请求体**：
JSON
```
{
  "type": "CREDIT_CARD",
  "token": "tok_1L9pZg2eZvKYlo2C..."
}


```


* **成功响应 (201 Created)**：
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

* **功能**：获取乘客绑定的支付方式列表。
* **身份验证**：需要具有乘客角色的 JWT。
* **成功响应 (200 OK)**：
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

* **功能**：删除已绑定的支付方式。
* **身份验证**：需要具有乘客角色的 JWT。
* **成功响应 (204 No Content)**

---

5. 匹配服务 (matching_service)

---

**基础路径**: `/api/matching`

#### `POST /driver/status`

* **功能**：司机更新自身状态。
* **身份验证**：需要具有司机角色的 JWT。
* **请求体**：
JSON
```
{
  "status": "ONLINE"
}


```


* **成功响应 (200 OK)**：
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

* **功能**：司机获取附近的可用订单列表。
* **身份验证**：需要具有司机角色的 JWT。
* **成功响应 (200 OK)**：
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
      "pickupLocation": { "address": "Jing'an Temple" },
      "dropoffLocation": { "address": "Hongqiao Airport T1" }
    }
  ]
}


```



#### `POST /driver/grab`

* **功能**：司机抢单。
* **身份验证**：需要具有司机角色的 JWT。
* **请求体**：
JSON
```
{
  "orderId": 1003
}


```


* **成功响应 (200 OK)**：
JSON
```
{
  "code": 0,
  "message": "Order grabbed successfully",
  "data": {
    "orderId": 1003,
    "matchStatus": "SUCCESS"
  }
}


```



---

6. 定位服务 (location_service)

---

**基础路径**: `/api/location`

#### `POST /driver/update`

* **功能**：司机上传实时位置信息。
* **身份验证**：需要具有司机角色的 JWT。
* **请求体**：
JSON
```
{
  "latitude": 31.2222,
  "longitude": 121.4581,
  "timestamp": 1678886400
}


```


* **成功响应 (200 OK)**：（无响应体）

#### `GET /order/{orderId}`

* **功能**：乘客获取进行中订单的司机实时位置。
* **身份验证**：需要具有乘客角色的 JWT。
* **成功响应 (200 OK)**：
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

* **功能**：（管理员）创建地理围栏。
* **身份验证**：需要管理员 JWT。
* **请求体**：
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


* **成功响应 (201 Created)**：（返回创建的 Geofence DTO）

#### `GET /`

* **功能**：（管理员）获取所有地理围栏。
* **身份验证**：需要管理员 JWT。
* **成功响应 (200 OK)**：（返回 Geofence DTO 列表）

---

7. 评价服务 (review_service)

---

**基础路径**: `/api/review`

#### `POST /`

* **功能**：提交评价。
* **身份验证**：需要有效的 JWT。
* **请求体**：
JSON
```
{
  "orderId": 1002,
  "rating": 5,
  "comment": "The driver's service attitude was great, and the car was clean and tidy!",
  "tags": ["Great Attitude", "Clean Car"]
}


```


* **成功响应 (201 Created)**：（返回创建的 review DTO）

#### `GET /order/{orderId}`

* **功能**：查看特定订单的评价。
* **身份验证**：需要有效的 JWT。
* **成功响应 (200 OK)**：（返回 review DTO 列表）

#### `POST /complaints`

* **功能**：提交投诉。
* **身份验证**：需要有效的 JWT。
* **请求体**：
JSON
```
{
  "orderId": 1004,
  "reason": "Dangerous Driving",
  "description": "The driver braked suddenly multiple times on the highway, which felt very unsafe.",
  "attachmentUrls": ["https://s3.bucket/video_proof.mp4"]
}


```


* **成功响应 (201 Created)**：（返回创建的 complaint DTO）

---

8. 分析服务 (analytics_service)

---

**基础路径**: `/api/analytics`

#### `GET /dashboard/summary`

* **功能**：（管理员）获取运营仪表盘的汇总数据。
* **身份验证**：需要管理员 JWT。
* **成功响应 (200 OK)**：
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

* **功能**：（管理员）生成指定类型的报表。
* **身份验证**：需要管理员 JWT。
* **请求体**：
JSON
```
{
  "reportType": "WEEKLY_GMV",
  "startDate": "2025-07-01",
  "endDate": "2025-07-07"
}


```


* **成功响应 (200 OK)**：（返回图表或时间序列数据点）

---

9. 管理后台服务 (admin_service)

---

**基础路径**: `/api/admin`

### 9.1. 用户管理 (`/api/admin/users`)

#### `GET /`

* **功能**：用户列表分页查询。
* **身份验证**：需要管理员 JWT。
* **成功响应 (200 OK)**：（返回分页用户列表）

#### `PUT /{userId}/status`

* **功能**：更改用户状态。
* **身份验证**：需要管理员 JWT。
* **请求体**：
JSON
```
{
  "status": "INACTIVE",
  "reason": "Violation of platform rules"
}


```


* **成功响应 (200 OK)**：
JSON
```
{ "code": 0, "message": "User status updated", "data": null }


```



### 9.2. 司机管理 (`/api/admin/drivers`)

#### `GET /applications`

* **功能**：查看待处理的司机入驻申请。
* **身份验证**：需要管理员 JWT。
* **成功响应 (200 OK)**：（返回分页的司机申请列表）

#### `POST /applications/{driverId}/approve`

* **功能**：批准司机的入驻申请。
* **身份验证**：需要管理员 JWT。
* **请求体**：
JSON
```
{
  "notes": "All documents verified."
}


```


* **成功响应 (200 OK)**：
JSON
```
{ "code": 0, "message": "Driver application approved", "data": null }


```



### 9.3. 订单与财务

#### `POST /orders/intervene`

* **功能**：管理员人工干预异常订单。
* **身份验证**：需要管理员 JWT。
* **请求体**：
JSON
```
{
  "orderId": 1005,
  "action": "FORCE_CANCEL",
  "reason": "Passenger emergency, cancellation without penalty."
}


```


* **成功响应 (200 OK)**：
JSON
```
{ "code": 0, "message": "Order intervention command sent", "data": null }


```



#### `GET /finance/withdrawals`

* **功能**：查看待处理的司机提现请求。
* **身份验证**：需要管理员 JWT。
* **成功响应 (200 OK)**：（返回分页的提现请求列表）

#### `POST /finance/withdrawals/{withdrawalId}/process`

* **功能**：处理提现请求。
* **身份验证**：需要管理员 JWT。
* **请求体**：
JSON
```
{
  "action": "APPROVE",
  "notes": "Payment processed, transaction ID T123456789"
}


```


* **成功响应 (200 OK)**：
JSON
```
{ "code": 0, "message": "Withdrawal request processed", "data": null }


```
