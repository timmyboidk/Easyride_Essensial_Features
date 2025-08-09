* * * * *

EasyRide Persistence Design Document
================

1. Overview
------

-   **Relational Database (RDBMS)**: **MySQL 8.0**

    -   Used for storing core business data requiring strong transactional consistency, such as user information, orders, and payment records.

-   **Cache Database**: **Redis**

    -   Used for caching hotspot data (e.g., user sessions, configuration information), storing temporary states (e.g., SMS verification codes, driver real-time locations), and serving as a supplement for distributed locks and message queues.

-   **Database Design Principles**:

    -   Each microservice owns an independent database (Database/Schema) to achieve data isolation, aligning with microservice architecture principles.

    -   Table and column names follow the `snake_case` naming convention (lowercase letters with underscores).

    -   All tables include `id` (primary key), `created_at` (creation time), `updated_at` (update time), and `version` (optimistic lock) fields.

    -   Foreign key relationships are indicated by appending `_id` to column names, e.g., `user_id`.

* * * * *

2. Detailed Database Design
-----------

### 2.1. User Service (`user_service_db`)

#### `users` Table

-   **Description**: Stores basic information for all users (including passengers and drivers).

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Primary key.

    -   `phone_number` (VARCHAR(20), UNIQUE, NOT NULL): Phone number for login.

    -   `hashed_password` (VARCHAR(255)): Encrypted password.

    -   `nickname` (VARCHAR(50)): Nickname.

    -   `avatar_url` (VARCHAR(255)): Avatar URL.

    -   `role` (ENUM('PASSENGER', 'DRIVER'), NOT NULL): User role.

    -   `status` (ENUM('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION'), NOT NULL): Account status.

    -   `registration_date` (DATETIME, NOT NULL): Registration date.

    -   `last_login_date` (DATETIME): Last login date.

#### `drivers` Table

-   **Description**: Stores detailed driver information and verification materials.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Primary key.

    -   `user_id` (BIGINT, FK -> `users.id`, UNIQUE, NOT NULL): Associated user ID.

    -   `real_name` (VARCHAR(50), NOT NULL): Real name.

    -   `id_card_number` (VARCHAR(30), NOT NULL): National ID number.

    -   `id_card_front_url` (VARCHAR(255), NOT NULL): Front photo URL of ID card.

    -   `id_card_back_url` (VARCHAR(255), NOT NULL): Back photo URL of ID card.

    -   `driver_license_number` (VARCHAR(30), NOT NULL): Driver's license number.

    -   `driver_license_url` (VARCHAR(255), NOT NULL): Driver's license photo URL.

    -   `car_model` (VARCHAR(50), NOT NULL): Vehicle model.

    -   `car_license_plate` (VARCHAR(20), NOT NULL): License plate number.

    -   `car_insurance_url` (VARCHAR(255), NOT NULL): Vehicle insurance policy URL.

    -   `verification_status` (ENUM('PENDING', 'APPROVED', 'REJECTED'), NOT NULL): Verification status.

    -   `service_rating_avg` (DECIMAL(3, 2)): Average service rating.

### 2.2. Order Service (`order_service_db`)

#### `orders` Table

-   **Description**: Stores core order information.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Order ID.

    -   `passenger_id` (BIGINT, NOT NULL, INDEX): Passenger's user ID.

    -   `driver_id` (BIGINT, INDEX): Driver's user ID (populated after order acceptance).

    -   `status` (ENUM('PENDING_MATCH', 'MATCHED', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'), NOT NULL): Order status.

    -   `service_type` (ENUM('AIRPORT_PICKUP', 'LONG_DISTANCE', 'CHARTER', 'CARPOOL'), NOT NULL): Service type.

    -   `pickup_location_address` (VARCHAR(255), NOT NULL): Pickup location address.

    -   `pickup_location_lat` (DECIMAL(10, 8), NOT NULL): Pickup location latitude.

    -   `pickup_location_lon` (DECIMAL(11, 8), NOT NULL): Pickup location longitude.

    -   `dropoff_location_address` (VARCHAR(255), NOT NULL): Dropoff location address.

    -   `dropoff_location_lat` (DECIMAL(10, 8), NOT NULL): Dropoff location latitude.

    -   `dropoff_location_lon` (DECIMAL(11, 8), NOT NULL): Dropoff location longitude.

    -   `estimated_fare` (DECIMAL(10, 2), NOT NULL): Estimated fare.

    -   `actual_fare` (DECIMAL(10, 2)): Actual fare.

    -   `pickup_time` (DATETIME): Pickup time.

    -   `dropoff_time` (DATETIME): Dropoff time.

    -   `created_time` (DATETIME, NOT NULL): Order creation time.

    -   `payment_status` (ENUM('UNPAID', 'PAID', 'REFUNDED'), NOT NULL, DEFAULT 'UNPAID'): Payment status.

### 2.3. Payment Service (`payment_service_db`)

#### `payments` Table

-   **Description**: Records each payment transaction.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Payment ID.

    -   `order_id` (BIGINT, NOT NULL, UNIQUE): Associated order ID.

    -   `user_id` (BIGINT, NOT NULL, INDEX): Paying user's ID.

    -   `amount` (DECIMAL(10, 2), NOT NULL): Payment amount.

    -   `status` (ENUM('PENDING', 'SUCCESS', 'FAILED'), NOT NULL): Payment status.

    -   `payment_method` (VARCHAR(50)): Payment method (e.g., 'CREDIT_CARD', 'PAYPAL').

    -   `transaction_id` (VARCHAR(255), UNIQUE): Third-party payment platform transaction ID.

    -   `paid_at` (DATETIME): Payment completion time.

#### `wallets` Table

-   **Description**: Stores driver wallet information.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Wallet ID.

    -   `driver_id` (BIGINT, FK -> `users.id`, UNIQUE, NOT NULL): Driver's user ID.

    -   `balance` (DECIMAL(12, 2), NOT NULL, DEFAULT 0.00): Wallet balance.

    -   `currency` (VARCHAR(3), NOT NULL, DEFAULT 'USD'): Currency unit.

#### `wallet_transactions` Table

-   **Description**: Records wallet income and expenditure details.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Transaction record ID.

    -   `wallet_id` (BIGINT, FK -> `wallets.id`, NOT NULL): Wallet ID.

    -   `type` (ENUM('INCOME', 'WITHDRAWAL', 'REFUND'), NOT NULL): Transaction type.

    -   `amount` (DECIMAL(10, 2), NOT NULL): Transaction amount.

    -   `related_order_id` (BIGINT): Associated order ID (if income).

    -   `related_withdrawal_id` (BIGINT): Associated withdrawal ID (if withdrawal).

    -   `status` (ENUM('COMPLETED', 'PENDING', 'FAILED'), NOT NULL): Transaction status.

    -   `transaction_date` (DATETIME, NOT NULL): Transaction date.

#### `withdrawals` Table

-   **Description**: Records driver withdrawal requests.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Withdrawal ID.

    -   `wallet_id` (BIGINT, FK -> `wallets.id`, NOT NULL): Wallet ID.

    -   `amount` (DECIMAL(10, 2), NOT NULL): Withdrawal amount.

    -   `status` (ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'COMPLETED'), NOT NULL): Withdrawal status.

    -   `request_time` (DATETIME, NOT NULL): Request time.

    -   `completion_time` (DATETIME): Completion time.

    -   `notes` (TEXT): Notes.

### 2.4. Review Service (`review_service_db`)

#### `reviews` Table

-   **Description**: Stores order review information.

-   **Columns**:

    -   `id` (BIGINT, PK, AUTO_INCREMENT): Review ID.

    -   `order_id` (BIGINT, NOT NULL, UNIQUE): Associated order ID.

    -   `reviewer_id` (BIGINT, NOT NULL): Reviewer's user ID.

    -   `reviewee_id` (BIGINT, NOT NULL): Reviewee's user ID.

    -   `rating` (TINYINT, NOT NULL): Rating (1-5).

    -   `comment` (TEXT): Text review.

    -   `review_time` (DATETIME, NOT NULL): Review time.

### 2.5. Redis Data Structures

#### Driver Real-Time Locations

-   **Type**: `GEOSPATIAL` (Geo)

-   **Key**: `driver:locations`

-   **Description**: Uses the `GEOADD` command to store latitude and longitude information for all online drivers. The `MEMBER` is the driver ID (`driver_id`). Enables quick lookup of nearby drivers using `GEORADIUS`.

#### SMS Verification Codes

-   **Type**: `STRING`

-   **Key**: `otp:login:{phone_number}`

-   **Description**: Stores the verification code for a phone number with a 5-minute expiration time (TTL).

#### User Sessions (JWT)

-   **Type**: `STRING` or `HASH`

-   **Key**: `session:user:{user_id}`

-   **Description**: Stores user JWT or session information for quick validation and forced logout.

#### Order Matching Lock

-   **Type**: `STRING` (used as a lock)

-   **Key**: `lock:order:match:{order_id}`

-   **Description**: Uses the `SETNX` (Set if Not Exists) command to implement a distributed lock, preventing multiple matching instances from processing the same order simultaneously.
