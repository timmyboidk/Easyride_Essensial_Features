# EasyRide Architecture Design

## 1. System Overview

EasyRide is a microservices-based ride-hailing platform designed for scalability, reliability, and ease of maintenance. The system is decomposed into loosely coupled services that communicate asynchronously via RocketMQ and synchronously via REST APIs.

### 1.1 Core Technologies
- **Backend**: Java 17, Spring Boot 3.4.1
- **Database**: MySQL 8.0 (Per-service isolation)
- **Caching**: Redis (GeoSpatial, Session, Locks)
- **Messaging**: Apache RocketMQ 5.x
- **Containerization**: Docker, Docker Compose
- **Build System**: Maven (Multi-Module)

## 2. Microservices

| Service | Description | Port |
|---------|-------------|------|
| **User Service** | User identity, profile, and driver verification. | 8081 |
| **Order Service** | Order lifecycle management (Creation, Status, Cancellation). | 8082 |
| **Payment Service** | Payment processing, wallets, and withdrawals. | 8083 |
| **Matching Service** | Driver-Passenger matching engine. | 8084 |
| **Location Service** | Real-time geo-tracking and maps integration. | 8085 |
| **Notification Service** | Multi-channel notifications (SMS, Email, Push). | 8086 |
| **Review Service** | Ratings and reviews system. | 8087 |
| **Analytics Service** | Data aggregation and business metrics. | 8088 |
| **Admin Service** | Back-office management dashboard. | 8080 |

## 3. Data Persistence Design

### 3.1 Relational Database (MySQL)
Each microservice owns its own database schema to ensure loose coupling.
- **Naming**: `snake_case` tables and columns.
- **Standard Fields**: `id` (PK), `created_at`, `updated_at`, `version` (Optimistic Lock).

#### Key Schemas
- **user_service_db**: `users`, `drivers`
- **order_service_db**: `orders`
- **payment_service_db**: `payments`, `wallets`, `transactions`, `withdrawals`
- **review_service_db**: `reviews`

### 3.2 Key-Value Store (Redis)
- **Driver Locations**: `GEOSPATIAL` at `driver:locations`
- **OTP Codes**: `STRING` at `otp:login:{phone}` (TTL 5 mins)
- **User Sessions**: `STRING` at `session:user:{id}`
- **Distributed Locks**: `SETNX` at `lock:order:match:{id}`

## 4. Message Queue Design (RocketMQ)

We use RocketMQ for domain event decoupling and eventual consistency.

### 4.1 Naming Conventions
- **Topic**: `EASYRIDE_{DOMAIN}_{ENTITY}_{ACTION}`
- **Producer Group**: `PID_{SERVICE_NAME}`
- **Consumer Group**: `CID_{SERVICE_NAME}`

### 4.2 Key Event Flows

#### Order Creation Flow
1. **Order Service** publishes `EASYRIDE_ORDER_CREATED_TOPIC`.
2. **Matching Service** consumes event -> Starts matching driver.
3. **Notification Service** consumes event -> Notifies user "Matching started".
4. **Analytics Service** consumes event -> Updates metrics.

#### Order Status Change Flow
1. **Order Service** updates status -> publishes `EASYRIDE_ORDER_STATUS_CHANGED_TOPIC`.
2. **Notification Service** notifies parties of status change (e.g., "Driver Arrived").
3. **Payment Service** listens for `COMPLETED` status to initiate payment logic.

#### Payment Success Flow
1. **Payment Service** confirms transaction -> publishes `EASYRIDE_PAYMENT_SUCCESS_TOPIC`.
2. **Order Service** updates order to `PAID`.
3. **Review Service** invites user to rate the ride.

## 5. Security & Consistency
- **Authentication**: JWT (JSON Web Tokens) for stateless auth.
- **Idempotency**: All MQ consumers and POST endpoints must handle duplicate requests gracefully (using `Idempotency-Key` or checking DB state).
- **Transactional Messages**: Used for critical cross-service consistency (e.g., Payment -> Order update).
