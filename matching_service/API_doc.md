EasyRide Backend API Documentation 
=======================

**Version**: 1.1

1. Introduction
------

### 1.1. General Design Principles

-   **Uniform Response Format**: All APIs return a standard JSON structure upon success.

    JSON

    ```
    {
      "code": 0,
      "message": "Success",
      "data": { ... }
    }

    ```

    On failure, `code` is a non-zero error code, `message` contains error information, and `data` is `null`.

-   **Authentication**: For most interfaces requiring user login, a `Bearer <JWT>` token must be included in the HTTP request's `Authorization` header.

-   **Security Signature**: For `POST`/`PUT`/`DELETE` requests involving funds, creation, or modification of critical data, the following fields must be included in the HTTP headers to prevent replay attacks and parameter tampering:

    -   `X-Timestamp`: Unix timestamp (in seconds) when the request was sent.

    -   `X-Nonce`: A one-time random string.

    -   `X-Signature`: HMAC-SHA256 signature calculated using `timestamp`, `nonce`, and the request body (or parameters) with a shared secret key.

-   **Idempotency**: For all `POST` requests creating or modifying resources, clients are advised to generate a unique `Idempotency-Key` in the request header to prevent duplicate creations due to network retries.

* * * * *

2. User Service (user_service)
-----------------------

**Base Path**: `/api/user`

### 2.1. Authentication and Registration (`/api/user/auth`)

#### `POST /register`

-   **Function**: Register a new user (passenger or driver).

-   **Request Body**:

    JSON

    ```
    {
      "phoneNumber": "13912345678",
      "password": "StrongPassword123!",
      "role": "PASSENGER"
    }

    ```

-   **Success Response (201 Created)**:

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

-   **Function**: Log in using phone number and password.

-   **Request Body**:

    JSON

    ```
    {
      "username": "13912345678",
      "password": "StrongPassword123!"
    }

    ```

-   **Success Response (200 OK)**:

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

-   **Function**: Log in using phone number and verification code.

-   **Request Body**:

    JSON

    ```
    {
      "phoneNumber": "13912345678",
      "otpCode": "654321"
    }

    ```

-   **Success Response (200 OK)**: (Same response structure as above)

#### `POST /otp/request`

-   **Function**: Request an SMS verification code.

-   **Request Body**:

    JSON

    ```
    {
      "phoneNumber": "13912345678"
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "OTP sent successfully.",
      "data": null
    }

    ```

#### `POST /password/reset`

-   **Function**: Reset password.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**:

    JSON

    ```
    {
      "oldPassword": "StrongPassword123!",
      "newPassword": "AnotherStrongPassword456!"
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    {
      "code": 0,
      "message": "Password has been reset successfully.",
      "data": null
    }

    ```

### 2.2. User Profile Management (`/api/user/profile`)

#### `GET /`

-   **Function**: Get the current logged-in user's profile.

-   **Authentication**: Requires a valid JWT.

-   **Success Response (200 OK)**:

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

-   **Function**: Update the current logged-in user's profile.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**:

    JSON

    ```
    {
      "nickname": "Rider VIP",
      "avatarUrl": "https://example.com/avatars/201_new.jpg"
    }

    ```

-   **Success Response (200 OK)**: (Same response structure as above, but with updated data)

### 2.3. Driver-Exclusive Features

#### `POST /driver/register`

-   **Function**: Submit a driver onboarding application.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**:

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

-   **Success Response (202 Accepted)**:

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

* * * * *

3. Order Service (order_service)
------------------------

**Base Path**: `/api/order`

#### `POST /`

-   **Function**: A passenger creates a new ride order.

-   **Authentication**: Requires a JWT with passenger role.

-   **Request Body**:

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

-   **Success Response (201 Created)**:

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

-   **Function**: Get detailed information for a specific order.

-   **Authentication**: Requires a JWT from the passenger or driver associated with the order.

-   **Success Response (200 OK)**:

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

-   **Function**: Update order status.

-   **Authentication**: Requires a JWT with driver role.

-   **Request Body**:

    JSON

    ```
    {
      "status": "DRIVER_ARRIVED"
    }

    ```

-   **Success Response (200 OK)**: (Same response structure as above, but `status` is updated)

#### `POST /{orderId}/cancel`

-   **Function**: Cancel an order.

-   **Authentication**: Requires a JWT from the passenger or driver associated with the order.

-   **Success Response (200 OK)**:

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

-   **Function**: Get the current user's historical order list.

-   **Authentication**: Requires a valid JWT.

-   **Success Response (200 OK)**:

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

-   **Function**: Estimate order price.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**: (Same structure as creating an order, but only necessary fields are required)

-   **Success Response (200 OK)**:

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

4. Payment Service (payment_service)
--------------------------

**Base Path**: `/api/payment`

### 4.1. Payment Processing (`/api/payment/payments`)

#### `POST /`

-   **Function**: Initiate payment for a specified order.

-   **Authentication**: Requires a JWT with passenger role.

-   **Request Body**:

    JSON

    ```
    {
      "orderId": 1002,
      "paymentMethodId": "pm_1L9pZg2eZvKYlo2C8c6t3XnZ"
    }

    ```

-   **Success Response (200 OK)**:

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

### 4.2. Wallet Management (`/api/payment/wallet`)

#### `GET /`

-   **Function**: Get the current user's wallet information (driver).

-   **Authentication**: Requires a JWT with driver role.

-   **Success Response (200 OK)**:

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

-   **Function**: Retrieve the wallet transaction history of the currently authenticated driver (paginated).
-   **Authentication**: JWT required for the driver role.
-   **Query Parameters**:
    -   `page` (int, optional, default 0): Page number.
    -   `size` (int, optional, default 20): Page size.
-   **Success Response (200 OK)**:
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

-   **Function**: Driver initiates a withdrawal request.

-   **Authentication**: Requires a JWT with driver role.

-   **Request Body**:

    JSON

    ```
    {
      "amount": 500.00,
      "withdrawalMethodId": "wth_paypal_account_123"
    }

    ```

-   **Success Response (202 Accepted)**:

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

### 4.3. Payment Method Management (`/api/payment/methods`)

#### `POST /`

-   **Function**: Passenger adds a new payment method.

-   **Authentication**: Requires a JWT with passenger role.

-   **Request Body**:

    JSON

    ```
    {
      "type": "CREDIT_CARD",
      "token": "tok_1L9pZg2eZvKYlo2C..."
    }

    ```

-   **Success Response (201 Created)**:

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

-   **Function**: Get the list of payment methods bound by the passenger.

-   **Authentication**: Requires a JWT with passenger role.

-   **Success Response (200 OK)**:

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

-   **Function**: Delete a bound payment method.

-   **Authentication**: Requires a JWT with passenger role.

-   **Success Response (204 No Content)**

* * * * *

5. Matching Service (matching_service)
---------------------------

**Base Path**: `/api/matching`

#### `POST /driver/status`

-   **Function**: Driver updates their own status.

-   **Authentication**: Requires a JWT with driver role.

-   **Request Body**:

    JSON

    ```
    {
      "status": "ONLINE"
    }

    ```

-   **Success Response (200 OK)**:

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

-   **Function**: Driver gets a list of nearby available orders.

-   **Authentication**: Requires a JWT with driver role.

-   **Success Response (200 OK)**:

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

-   **Function**: Driver grabs an order.

-   **Authentication**: Requires a JWT with driver role.

-   **Request Body**:

    JSON

    ```
    {
      "orderId": 1003
    }

    ```

-   **Success Response (200 OK)**:

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

* * * * *

6. Location Service (location_service)
---------------------------

**Base Path**: `/api/location`

#### `POST /driver/update`

-   **Function**: Driver uploads their real-time location information.

-   **Authentication**: Requires a JWT with driver role.

-   **Request Body**:

    JSON

    ```
    {
      "latitude": 31.2222,
      "longitude": 121.4581,
      "timestamp": 1678886400
    }

    ```

-   **Success Response (200 OK)**: (No response body)

#### `GET /order/{orderId}`

-   **Function**: Passenger gets the real-time location of the driver for an ongoing order.

-   **Authentication**: Requires a JWT with passenger role.

-   **Success Response (200 OK)**:

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

### 6.1. Geofence Management (`/api/location/geofences`)

#### `POST /`

-   **Function**: (Admin) Create a geofence.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

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

-   **Success Response (201 Created)**: (Returns the created Geofence DTO)

#### `GET /`

-   **Function**: (Admin) Get all geofences.

-   **Authentication**: Requires admin JWT.

-   **Success Response (200 OK)**: (Returns a list of Geofence DTOs)

* * * * *

7. Review Service (review_service)
-------------------------

**Base Path**: `/api/review`

#### `POST /`

-   **Function**: Submit a review.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**:

    JSON

    ```
    {
      "orderId": 1002,
      "rating": 5,
      "comment": "The driver's service attitude was great, and the car was clean and tidy!",
      "tags": ["Great Attitude", "Clean Car"]
    }

    ```

-   **Success Response (201 Created)**: (Returns the created review DTO)

#### `GET /order/{orderId}`

-   **Function**: View reviews for a specific order.

-   **Authentication**: Requires a valid JWT.

-   **Success Response (200 OK)**: (Returns a list of review DTOs)

#### `POST /complaints`

-   **Function**: Submit a complaint.

-   **Authentication**: Requires a valid JWT.

-   **Request Body**:

    JSON

    ```
    {
      "orderId": 1004,
      "reason": "Dangerous Driving",
      "description": "The driver braked suddenly multiple times on the highway, which felt very unsafe.",
      "attachmentUrls": ["https://s3.bucket/video_proof.mp4"]
    }

    ```

-   **Success Response (201 Created)**: (Returns the created complaint DTO)

* * * * *

8. Analytics Service (analytics_service)
------------------------------

**Base Path**: `/api/analytics`

#### `GET /dashboard/summary`

-   **Function**: (Admin) Get summary data for the operations dashboard.

-   **Authentication**: Requires admin JWT.

-   **Success Response (200 OK)**:

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

-   **Function**: (Admin) Generate a report of the specified type.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

    JSON

    ```
    {
      "reportType": "WEEKLY_GMV",
      "startDate": "2025-07-01",
      "endDate": "2025-07-07"
    }

    ```

-   **Success Response (200 OK)**: (Returns chart or time-series data points)

* * * * *

9. Admin Backend Service (admin_service)
--------------------------

**Base Path**: `/api/admin`

### 9.1. User Management (`/api/admin/users`)

#### `GET /`

-   **Function**: Paginated query of user list.

-   **Authentication**: Requires admin JWT.

-   **Success Response (200 OK)**: (Returns paginated user list)

#### `PUT /{userId}/status`

-   **Function**: Change user status.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

    JSON

    ```
    {
      "status": "INACTIVE",
      "reason": "Violation of platform rules"
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "User status updated", "data": null }

    ```

### 9.2. Driver Management (`/api/admin/drivers`)

#### `GET /applications`

-   **Function**: View pending driver onboarding applications.

-   **Authentication**: Requires admin JWT.

-   **Success Response (200 OK)**: (Returns paginated driver applications)

#### `POST /applications/{driverId}/approve`

-   **Function**: Approve a driver's onboarding application.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

    JSON

    ```
    {
      "notes": "All documents verified."
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "Driver application approved", "data": null }

    ```

### 9.3. Orders and Finance

#### `POST /orders/intervene`

-   **Function**: Admin manually intervenes in abnormal orders.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

    JSON

    ```
    {
      "orderId": 1005,
      "action": "FORCE_CANCEL",
      "reason": "Passenger emergency, cancellation without penalty."
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "Order intervention command sent", "data": null }

    ```

#### `GET /finance/withdrawals`

-   **Function**: View pending driver withdrawal requests.

-   **Authentication**: Requires admin JWT.

-   **Success Response (200 OK)**: (Returns paginated withdrawal requests)

#### `POST /finance/withdrawals/{withdrawalId}/process`

-   **Function**: Process a withdrawal request.

-   **Authentication**: Requires admin JWT.

-   **Request Body**:

    JSON

    ```
    {
      "action": "APPROVE",
      "notes": "Payment processed, transaction ID T123456789"
    }

    ```

-   **Success Response (200 OK)**:

    JSON

    ```
    { "code": 0, "message": "Withdrawal request processed", "data": null }

    ```
