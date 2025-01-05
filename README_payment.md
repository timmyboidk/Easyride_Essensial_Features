Payment Service
===============

技术栈
---

-   **编程语言**：Java 17
-   **框架**：Spring Boot 3.x
-   **安全**：Spring Security
-   **持久层**：Spring Data JPA
-   **数据库**：MySQL
-   **身份验证**：JWT（JSON Web Token）
-   **依赖管理**：Maven
-   **其他工具**：Lombok（用于简化代码）、Kafka（用于与其他微服务的异步通信）
-   **支付网关**：PayPal SDK 和 Stripe SDK（用于信用卡支付）

* * * * *

关键类、变量和方法
---------

### 1\. 模型层（Model）

#### **`Payment` 类**

表示支付实体，映射到数据库中的 `payments` 表。

-   **关键变量**：
    -   `Long id`：支付的唯一标识符，主键。
    -   `Long orderId`：关联的订单 ID。
    -   `Long passengerId`：付款人的 ID。
    -   `Double amount`：支付金额。
    -   `PaymentStatus status`：支付状态（`PENDING`、`COMPLETED`、`FAILED`、`REFUNDED`）。
    -   `TransactionType transactionType`：交易类型（支付或退款）。
    -   `LocalDateTime createdAt`：支付创建时间。
    -   `LocalDateTime updatedAt`：支付更新时间。

#### **`Wallet` 类**

表示司机钱包实体，存储司机的账户余额。

-   **关键变量**：
    -   `Long driverId`：司机的唯一标识符。
    -   `Double balance`：司机的钱包余额。
    -   `LocalDateTime updatedAt`：钱包更新时间。

#### **`Withdrawal` 类**

表示提现请求实体。

-   **关键变量**：
    -   `Long id`：提现的唯一标识符，主键。
    -   `Long driverId`：司机的唯一标识符。
    -   `Double amount`：提现金额。
    -   `WithdrawalStatus status`：提现状态（`PENDING`、`APPROVED`、`REJECTED`、`PROCESSED`）。
    -   `String bankAccount`：绑定的银行账户。
    -   `LocalDateTime requestedAt`：提现请求时间。
    -   `LocalDateTime processedAt`：提现处理时间。

**对应需求**：

-   **支付记录和状态管理**：通过 `Payment` 类管理订单支付记录。
-   **司机钱包管理**：通过 `Wallet` 类管理司机的收入结算。
-   **提现管理**：通过 `Withdrawal` 类管理司机的提现请求和状态。

* * * * *

### 2\. 数据传输对象（DTO）

#### **`PaymentRequestDto` 类**

用于封装支付请求的数据。

-   **关键变量**：
    -   `Long orderId`：订单 ID。
    -   `Long passengerId`：付款人 ID。
    -   `Double amount`：支付金额。
    -   `String paymentMethod`：支付方式（`PAYPAL`、`CREDIT_CARD`、`BALANCE`）。
    -   `String currency`：货币类型（如 USD、CNY 等）。

#### **`WalletDto` 类**

用于返回司机钱包信息的数据。

-   **关键变量**：
    -   `Long driverId`：司机 ID。
    -   `Double balance`：钱包余额。
    -   `LocalDateTime updatedAt`：钱包更新时间。

#### **`WithdrawalRequestDto` 类**

用于封装提现请求的数据。

-   **关键变量**：
    -   `Long driverId`：司机 ID。
    -   `Double amount`：提现金额。
    -   `String bankAccount`：提现的银行账户信息。

**对应需求**：

-   **支付请求和处理**：通过 `PaymentRequestDto` 接收支付数据。
-   **司机钱包管理**：通过 `WalletDto` 提供司机余额信息。
-   **提现请求**：通过 `WithdrawalRequestDto` 进行提现数据的传递。

* * * * *

### 3\. 仓库层（Repository）

#### **`PaymentRepository` 接口**

继承自 `JpaRepository`，用于对 `Payment` 实体进行数据库操作。

#### **`WalletRepository` 接口**

用于对 `Wallet` 实体进行数据库操作。

#### **`WithdrawalRepository` 接口**

用于对 `Withdrawal` 实体进行数据库操作。

**关键方法**：

-   `List<Payment> findByPassengerId(Long passengerId)`：根据乘客 ID 查找支付记录。
-   `List<Withdrawal> findByDriverId(Long driverId)`：根据司机 ID 查找提现记录。

**对应需求**：

-   **数据持久化**：通过仓库层管理支付记录、司机钱包余额和提现请求的数据操作。

* * * * *

### 4\. 服务层（Service）

#### **`PaymentService` 接口**

定义了支付服务的抽象方法。

-   **关键方法**：
    -   `PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto)`：处理支付请求。
    -   `void handlePaymentNotification(Map<String, String> notificationData)`：处理支付网关的异步通知。
    -   `void refundPayment(Long paymentId, Double amount)`：处理退款请求。

#### **`WalletService` 接口**

定义了司机钱包服务的抽象方法。

-   **关键方法**：
    -   `void addFunds(Long orderId, Double amount)`：为司机钱包增加收入。
    -   `WalletDto getWallet(Long driverId)`：获取司机钱包信息。
    -   `List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to)`：获取司机收入记录。

#### **`WithdrawalService` 接口**

定义了提现服务的抽象方法。

-   **关键方法**：
    -   `WithdrawalResponseDto requestWithdrawal(WithdrawalRequestDto withdrawalRequestDto)`：处理提现申请。
    -   `void processWithdrawal(Long withdrawalId)`：处理提现请求。
    -   `List<Withdrawal> getWithdrawalHistory(Long driverId)`：获取司机的提现记录。

**对应需求**：

-   **支付处理和管理**：使用 `PaymentService` 实现支付处理、异步通知处理和退款。
-   **司机钱包管理**：使用 `WalletService` 实现司机的收入结算和查询。
-   **提现处理**：使用 `WithdrawalService` 实现司机的提现申请和处理。

* * * * *

### 5\. 控制器层（Controller）

#### **`PaymentController` 类**

处理与支付相关的 HTTP 请求。

-   **关键方法**：
    -   `PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto)`：处理支付请求。
    -   `void handlePaymentNotification(Map<String, String> notificationData)`：接收支付网关的异步通知。
    -   `void refundPayment(Long paymentId, Double amount)`：处理退款请求。

#### **`WalletController` 类**

处理与司机钱包相关的 HTTP 请求。

-   **关键方法**：
    -   `WalletDto getWallet(Long driverId)`：获取司机钱包信息。
    -   `List<Payment> getEarnings(Long driverId, LocalDateTime from, LocalDateTime to)`：获取司机的收入记录。

#### **`WithdrawalController` 类**

处理与提现相关的 HTTP 请求。

-   **关键方法**：
    -   `WithdrawalResponseDto requestWithdrawal(WithdrawalRequestDto withdrawalRequestDto)`：提交提现申请。
    -   `List<Withdrawal> getWithdrawalHistory(Long driverId)`：获取司机的提现记录。

**对应需求**：

-   **API 接口**：提供支付、钱包管理和提现管理的 RESTful API。

* * * * *

### 6\. 安全配置（Security）

#### **`SecurityConfig` 类**

配置 Spring Security。

-   **关键配置**：
    -   禁用 CSRF。
    -   配置请求权限，保护 `/payments/**`、`/wallets/**`、`/withdrawals/**` 路径下的接口。
    -   配置 JWT 认证过滤器。

#### **`JwtTokenProvider` 类**

生成和验证 JWT 令牌。

#### **`JwtAuthenticationFilter` 类**

JWT 认证过滤器，拦截请求并进行身份验证。

**对应需求**：

-   **身份验证和授权**：通过 JWT 实现无状态的身份验证，保护支付、钱包和提现相关的 API 接口。

* * * * *

### 7\. 工具类（Utility）

#### **`PaymentGatewayUtil` 类**

用于与支付网关（如 PayPal 和 Stripe）进行通信。

-   **关键方法**：
    -   `boolean processPayment(PaymentRequestDto paymentRequestDto)`：处理支付请求。
    -   `boolean refundPayment(Long paymentId, Double amount)`：处理退款请求。

**对应需求**：

-   **支付处理**：通过调用支付网关的 API，处理支付和退款请求。


示例请求和响应
-------

### 1\. 发起支付

-   **请求**：`POST /payments/pay`

-   **请求头**：



    `Authorization: Bearer your_jwt_token
    Content-Type: application/json`

-   **请求体**：



    `{
      "orderId": 100,
      "passengerId": 1,
      "amount": 100.0,
      "paymentMethod": "CREDIT_CARD",
      "currency": "USD"
    }`

-   **响应**：



    `{
      "paymentId": 500,
      "status": "COMPLETED",
      "message": "支付成功"
    }`

### 2\. 接收支付网关的异步通知

-   **请求**：`POST /payments/notify`

-   **请求头**：



    `Content-Type: application/json`

-   **请求体**：


    `{
      "paymentId": "500",
      "status": "COMPLETED"
    }`

-   **响应**：



    `HTTP/1.1 200 OK`

### 3\. 退款请求

-   **请求**：`POST /payments/refund/500?amount=50.0`

-   **请求头**：



    `Authorization: Bearer your_jwt_token
    Content-Type: application/json`

-   **请求体**：无

-   **响应**：



    `HTTP/1.1 200 OK`

### 4\. 获取司机钱包信息

-   **请求**：`GET /wallets/2`

-   **请求头**：



    `Authorization: Bearer your_jwt_token`

-   **响应**：



    `{
      "driverId": 2,
      "balance": 450.0,
      "updatedAt": "2024-04-27T10:15:30"
    }`

### 5\. 获取司机收入记录

-   **请求**：`GET /wallets/2/earnings?from=2024-04-01T00:00:00&to=2024-04-30T23:59:59`

-   **请求头**：



    `Authorization: Bearer your_jwt_token`

-   **响应**：



    `[
      {
        "id": 600,
        "orderId": 100,
        "passengerId": 1,
        "amount": 100.0,
        "status": "COMPLETED",
        "transactionType": "PAYMENT",
        "createdAt": "2024-04-15T14:30:00",
        "updatedAt": "2024-04-15T14:35:00"
      },
      {
        "id": 601,
        "orderId": 101,
        "passengerId": 2,
        "amount": 200.0,
        "status": "COMPLETED",
        "transactionType": "PAYMENT",
        "createdAt": "2024-04-20T09:20:00",
        "updatedAt": "2024-04-20T09:25:00"
      }
    ]`

### 6\. 发起提现申请

-   **请求**：`POST /withdrawals/request`

-   **请求头**：



    `Authorization: Bearer your_jwt_token
    Content-Type: application/json`

-   **请求体**：



    `{
      "driverId": 2,
      "amount": 100.0,
      "bankAccount": "1234567890"
    }`

-   **响应**：



    `{
      "withdrawalId": 700,
      "status": "PENDING",
      "message": "提现申请已提交"
    }`

### 7\. 处理提现申请

-   **请求**：`POST /withdrawals/process/700`

-   **请求头**：



    `Authorization: Bearer admin_jwt_token`

-   **请求体**：无

-   **响应**：



    `HTTP/1.1 200 OK`

### 8\. 获取提现记录

-   **请求**：`GET /withdrawals/2/history`

-   **请求头**：



    `Authorization: Bearer your_jwt_token`

-   **响应**：



    `[
      {
        "id": 700,
        "driverId": 2,
        "amount": 100.0,
        "status": "PROCESSED",
        "bankAccount": "1234567890",
        "requestedAt": "2024-04-25T11:00:00",
        "processedAt": "2024-04-27T10:00:00"
      },
      {
        "id": 701,
        "driverId": 2,
        "amount": 200.0,
        "status": "REJECTED",
        "bankAccount": "0987654321",
        "requestedAt": "2024-04-26T12:00:00",
        "processedAt": "2024-04-27T11:00:00"
      }
    ]`

### 9\. 获取支付记录

-   **请求**：`GET /payments/order/100`

-   **请求头**：


    `Authorization: Bearer your_jwt_token`

-   **响应**：


    `[
      {
        "id": 500,
        "orderId": 100,
        "passengerId": 1,
        "amount": 100.0,
        "status": "COMPLETED",
        "transactionType": "PAYMENT",
        "createdAt": "2024-04-15T14:30:00",
        "updatedAt": "2024-04-15T14:35:00"
      }
    ]`
