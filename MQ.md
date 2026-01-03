EasyRide Message Queue (MQ) Design Document
=======================

1\. Overview
------

-   **Message Broker**: **Apache RocketMQ**

-   **Core Functions**:

    -   **Asynchronous Processing**: Offloads time-consuming operations (e.g., sending notifications, generating reports) to improve API response times and user experience.

    -   **Service Decoupling**: Eliminates direct synchronous service calls, reducing system coupling while enhancing scalability and fault tolerance.

    -   **Eventual Consistency**: Ensures eventual data consistency for distributed transactions through reliable messaging mechanisms.

-   **Naming Conventions**:

    -   **Topic**: `EASYRIDE_{Business Domain}_{Message Entity}_{Action}`

        -   Example: `EASYRIDE_ORDER_CREATED_TOPIC`

    -   **Producer Group**: `PID_{Source Service Name}`

        -   Example: `PID_ORDER_SERVICE`

    -   **Consumer Group**: `CID_{Target Service Name}`

        -   Example: `CID_NOTIFICATION_SERVICE`

* * * * *

2\. Message Topics and Their Producers/Consumers
---------------------------

### 2.1. Order Domain

#### **Topic: `EASYRIDE_ORDER_CREATED_TOPIC`**

-   **Description**: Published when a new order is created.

-   **Payload**: `OrderCreatedEvent` (includes order ID, passenger ID, origin, destination, service type, etc.)

-   **Producers**:

    -   **`order-service`** (`PID_ORDER_SERVICE`): Sends after the order is successfully stored.

-   **Consumers**:

    -   **`matching-service`** (`CID_MATCHING_SERVICE`): Upon receiving the message, begins searching for a suitable driver for the order.

    -   **`notification-service`** (`CID_NOTIFICATION_SERVICE`): Sends passengers a notification stating “Order created, matching a driver for you.”

    -   **`analytics-service`** (`CID_ANALYTICS_SERVICE`): Receives messages to update real-time metrics like “Today's Order Count.”

#### **Topic: `EASYRIDE_ORDER_STATUS_CHANGED_TOPIC`**

-   **Description**: Published whenever the order status changes (e.g., driver matched, driver arrived, trip started/ended, canceled).

-   **Payload**: `OrderStatusChangedEvent` (includes order ID, old/new status, driver ID, timestamp, etc.)

-   **Producers**:

    -   **`order-service`** (`PID_ORDER_SERVICE`): Sends after updating order status.

-   **Consumers**:

    -   **`notification-service`** (`CID_NOTIFICATION_SERVICE`): Sends real-time notifications to passengers and drivers based on different status changes.

    -   **`payment-service`** (`CID_PAYMENT_SERVICE`): Triggers automatic payment deduction or generates invoices when status changes to `COMPLETED`.

    -   **`analytics-service`** (`CID_ANALYTICS_SERVICE`): Updates order funnel analytics and related data.

### 2.2. Payment Domain

#### **Topic: `EASYRIDE_PAYMENT_SUCCESS_TOPIC`**

-   **Description**: Published when a payment is successfully completed.

-   **Payload**: `PaymentSuccessEvent` (includes payment ID, order ID, amount, payment time, etc.)

-   **Producers**:

    -   **`payment-service`** (`PID_PAYMENT_SERVICE`): Sends after confirming payment success.

-   **Consumers**:

    -   **`order-service`** (`CID_ORDER_SERVICE`): Upon receiving the message, updates the `payment_status` of the corresponding order to `PAID`.

    -   **`notification-service`** (`CID_NOTIFICATION_SERVICE`): Sends a payment success notification to the passenger and a “Passenger has paid” notification to the driver.

    -   **`review-service`** (`CID_REVIEW_SERVICE`): Triggers the “Invite to Review” logic.

### 2.3. User Domain

#### **Topic: `EASYRIDE_USER_REGISTERED_TOPIC`**

-   **Description**: Published when a new user (passenger or driver) completes registration.

-   **Payload**: `UserRegisteredEvent` (includes user ID, role, registration time, etc.)

-   **Producers**:

    -   **`user-service`** (`PID_USER_SERVICE`): Sends after user data is written to the database.

-   **Consumers**:

    -   **`notification-service`** (`CID_NOTIFICATION_SERVICE`): Sends welcome SMS or email.

    -   **`analytics-service`** (`CID_ANALYTICS_SERVICE`): Updates the “Newly Registered Users” metric.

#### **Topic: `EASYRIDE_DRIVER_APPLICATION_SUBMITTED_TOPIC`**

-   **Description**: Published when a driver submits an application.

-   **Payload**: `DriverApplicationEvent` (includes driver user ID, application materials link, etc.)

-   **Producers**:

    -   **`user-service`** (`PID_USER_SERVICE`): Sends after driver submits application.

-   **Consumers**:

    -   **`admin-service`** (`CID_ADMIN_SERVICE`): Receives the message and creates a new review task in the “Pending Review List” of the admin dashboard.

    -   **`notification-service`** (`CID_NOTIFICATION_SERVICE`): Notifies relevant reviewers that a new application is pending processing.

### 2.4. Admin Domain

#### **Topic: `EASYRIDE_ADMIN_OPERATION_LOG_TOPIC`**

-   **Description**: Records all critical operations performed by administrators in the backend.

-   **Payload**: `AdminLogEvent` (includes admin ID, operation type, target entity ID, operation details, IP address, etc.)

-   **Producers**:

    -   **`admin-service`** (`PID_ADMIN_SERVICE`): Sends after performing any sensitive operations (e.g., disabling users, approving withdrawals, intervening in orders).

-   **Consumers**:

    -   **`analytics-service`** (`CID_ANALYTICS_SERVICE`): Stores log information in a log storage system (e.g., ELK Stack) for auditing and security monitoring.

* * * * *

3\. Message Reliability Assurance
--------- --

-   **Producers**:

    -   Employ **Sync Send** mode to ensure messages are successfully delivered to the Broker.

    -   For highly critical operations (e.g., payments), utilize **Transactional Messages** to guarantee atomicity between local transactions (e.g., database updates) and message delivery.

-   **Consumer**:

    -   All consumers must implement **idempotency**. Prevent duplicate consumption by checking if the business ID (e.g., order ID) has already been processed.

    -   Upon successful consumption, return the `CONSUME_SUCCESS` status to the Broker.

    -   If consumption fails, return `RECONSUME_LATER`. RocketMQ will retry based on the configured retry policy. When the retry limit is reached, the message is delivered to the **Dead-Letter Queue (DLQ)**.

-   **Monitoring**:

    -   Configure dedicated monitoring to track the Dead-Letter Queue. When messages exist in the DLQ, trigger alerts to enable manual intervention by developers.

Translated with DeepL.com (free version)
