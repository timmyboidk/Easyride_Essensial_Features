* * * * *

EasyRide Microservices Project README
=====================

1\. Project Overview
--------

**EasyRide** is a transportation platform built using a microservices architecture, dedicated to providing users with efficient, safe, and convenient travel services. The system comprises a series of distinct microservices that can be independently deployed and scaled. Asynchronous communication and decoupling are achieved through the **RocketMQ** message queue, ensuring high availability and elasticity.

2\. Core Microservice Modules
-----------

The entire platform consists of the following core microservices, each bearing specific business responsibilities:

### 2.1. User Service (user_service)

-   **Core Responsibilities**: Serves as the unified management center for all users (passengers and drivers), overseeing the entire user lifecycle.

-   **Primary Functions**:

    -   **User Registration and Authentication**: Supports passenger and driver registration, providing secure identity verification via JWT (JSON Web Tokens).

    -   **Profile Management**: Enables users to view and update personal information.

    -   **Event Publishing**: Publishes key user-related actions (e.g., registration, profile updates) as events to RocketMQ for consumption by other services.

### 2.2. Order Service (order_service)

-   **Core Responsibility**: Manages the entire order lifecycle from creation and dispatch to final completion.

-   **Primary Functions**:

    -   **Order Creation**: Receives order requests from passengers and performs preliminary cost and time estimates.

    -   **Status Management**: Precisely tracks and manages various order status transitions (e.g., Pending Acceptance, In Transit, Completed).

    -   **Driver Dispatch**: Match the most suitable driver based on distance, vehicle type, and other criteria.

    -   **Event-Driven**: Enable efficient asynchronous communication with user, payment, and matching services via RocketMQ.

### 2.3. Payment Service (payment_service)

-   **Core Responsibilities**: Centralizes all payment-related operations for the platform, including passenger payments, driver wallet management, and withdrawal functionality.

-   **Primary Functions**:

    -   **Payment Processing**: Supports multiple payment channels to handle passenger payment requests.

    -   **Wallet Management**: Provides drivers with an in-app wallet to track earnings details.

    -   **Withdrawal Functionality**: Enables drivers to transfer wallet balances to linked bank accounts.

    -   **Security Measures**: Implements application-layer encryption, Redis jittering, MD5 signatures, and anti-tampering protocols to ensure transaction security and idempotency.

### 2.4. Matching Service (matching_service)

-   **Core Functionality**: Serves as the intelligent brain for order allocation, efficiently matching the most suitable driver based on passenger order requirements and real-time driver status.

-   **Key Features**:

    -   **Automatic Matching**: Smartly recommends the optimal driver for each order through comprehensive multi-dimensional calculations including distance, rating, and vehicle type.

    -   **Manual Order Acceptance & Bidding**: Supports drivers browsing and selecting orders manually or participating in bidding mode.

    -   **Status Synchronization**: Synchronizes order and driver statuses in real-time with `order_service` and `user_service` via message queues.

### 2.5. Location Service (location_service)

-   **Core Responsibilities**: Provides all location-related functionalities.

-   **Primary Features**:

    -   **Real-Time Location Tracking**: Captures and updates driver and passenger locations in real time.

    -   **Location Query and Map Services**: Integrates with map providers (Google Maps) to enable route planning, distance calculation, and more.

    -   **Geofencing**: Supports setting service areas, special fare zones, or restricted zones.

### 2.6. Notification Service (notification_service)

-   **Core Responsibilities**: Handles all external communication notifications for the platform.

-   **Primary Functions**:

    -   **Event-Driven**: Monitors business events published by other microservices (e.g., order, payment, user services).

    -   **Multi-Channel Distribution**: Delivers notifications via SMS, email, and mobile push (APNs for iOS, FCM for Android) based on event type and user preferences.

    -   **Template-Based Content**: Generates dynamic, localized notification content using a template engine.

### 2.7. Review Service (review_service)

-   **Core Responsibilities**: Manages the two-way rating system between users (passengers and drivers).

-   **Primary Functions**:

    -   **Ratings and Reviews**: Enables passengers and drivers to rate each other and provide text reviews after trips conclude.

    -   **Complaints and Appeals**: Provides a process for users to file complaints about unsatisfactory service and allows the complained-about party to submit an appeal.

    -   **Content Moderation**: Includes a sensitive word filtering mechanism to ensure platform content compliance.

### 2.8. Analytics Service (analytics_service)

-   **Core Responsibilities**: Serves as the platform's data hub, responsible for collecting and analyzing all operational data to support business decision-making.

-   **Key Features**:

    -   **Data Collection**: Captures incremental data in real-time by monitoring business events published by microservices.

    -   **Metrics Management & Reporting**: Supports analysis of multi-dimensional key metrics such as order volume, active users, and transaction value, generating reports.

    -   **Data Visualization**: Provides data query interfaces to support data visualization in the admin dashboard.

    -   **Privacy Protection**: Sensitive information undergoes de-identification before storage to ensure user data security.

### 2.9. Admin Backend Service (admin_service)

-   **Core Responsibilities**: Provides platform operators with a unified management and monitoring backend system.

-   **Primary Functions**:

    -   **User Management**: View and review all user information, handle non-compliant accounts.

    -   **Order Monitoring & Intervention**: Real-time tracking of order statuses with manual intervention capabilities (e.g., reallocation or cancellation).

    -   **Financial Management**: Review platform revenue streams and manage driver withdrawal requests.

    -   **System Configuration**: Administer platform rules including pricing, commission rates, notification templates, etc.

3\. Technology Stack
-----------
**Backend Framework**

Spring Boot 3.x

Foundational framework for microservices

**Programming Language**

Java 17

Primary development language

**Data Persistence**

Spring Data JPA, MySQL

Relational data storage

**Message Queue**

RocketMQ

Asynchronous communication and decoupling between microservices

**Security Authentication**

Spring Security, JWT

API security and user authentication

**Dependency Management**

Maven

Project build and dependency management

**Development Tools**

Lombok

Simplifies Java code writing

**Payment Gateways**

PayPal SDK, Stripe SDK

Processes online payments like credit cards
