# Frontend Developer Guide

## Overview
This guide provides instructions for frontend developers (Web, iOS, Android) on how to integrate with the EasyRide backend microservices.

## 1. Authentication & Security
The system uses **JWT (JSON Web Tokens)** for authentication.

### Flow
1. **Login**: Call `POST /users/login` with credentials.
2. **Receive Token**: Response details:
   ```json
   {
       "code": 0,
       "data": {
           "token": "eyJhbGciOi...",
           "type": "Bearer"
       }
   }
   ```
3. **Storage**: Securely store the token (e.g., Keychain, SecureSharedPreferences, HttpOnly Cookie).
4. **Authenticated Requests**: Add the token to the header of every subsequent request.
   ```http
   Authorization: Bearer <your_token>
   ```

### Role-Based Access
- Some endpoints are restricted to `DRIVER` or `ADMIN` roles.
- Ensure the user has the correct role before attempting to access restricted features to avoid `403 Forbidden` errors.

## 2. API Response Standard
All APIs (except some legacy notifications) follow a standard envelope format:

```typescript
interface ApiResponse<T> {
  code: number;      // 0 represents success. Any other number indicates an error code.
  message: string;   // User-friendly message (e.g., "Order Created").
  data: T;           // The actual data payload. Null if error.
  timestamp: number; // Server time.
}
```

**Error Handling Strategy:**
- If `code !== 0`, display `message` to the user (e.g., in a Toast/Snackbar).
- Handle `401 Unauthorized` by redirecting to the Login Screen.

## 3. Key Workflows

### A. Ordering a Ride (Passenger)
1. **Get Location**: Use native device GPS to get lat/lon.
2. **Reverse Geocode (Optional)**: Call `GET /api/location/info?lat=...&lon=...` to get a readable address.
3. **Create Order**: Call `POST /orders/create` with:
   - `startLocation`, `endLocation`
   - `vehicleType`, `serviceType`
4. **Polling/Socket**: Connect to the notification socket (or poll `GET /orders/{id}`) to list status changes:
   - `PENDING_MATCH` -> `DRIVER_ASSIGNED` -> `ARRIVED` -> `IN_PROGRESS` -> `COMPLETED`.

### B. Accepting a Ride (Driver)
1. **Go Online**: Update status via `POST /matching/driverStatus/{id}`.
2. **Receive Request**: Listen for Push Notification or Poll `GET /matching/orders/available`.
3. **Accept**: Call `POST /orders/{orderId}/accept`.
4. **Update Trip**: Call `POST /orders/{orderId}/status` to update progress (arrived, started, finished).

### C. Payment
**Important**: The payment API requires **Encryption**.
1. Construct your JSON payload: `{"orderId": 123, "amount": 25.0, ...}`
2. Encrypt this string using the shared public key/secret.
3. Send `POST /payments/pay` with body: `{ "payload": "<encrypted_string>" }`.
4. Decrypt the response `payload` to get the result.

## 4. Environment Setup
- **Base Domain**: 
  - Local Dev: `http://localhost:<ServicePort>` (See API_REFERENCE.md for ports)
  - Staging: `https://api.staging.easyride.com` (Example)
- **CORS**: The backend is configured to allow requests from standard frontend ports (3000, 8080). If you face CORS issues, ensure you are sending the `Origin` header correctly.

## 5. WebSocket / Real-time Updates
*(If applicable)*
- The backend supports RocketMQ for internal messaging.
- For frontend real-time updates (e.g., driver location on map), we recommend using the **Push Notification** flow (`/send-push`) or polling the **Location Service** (`/api/location/trip/{orderId}/path`) every few seconds.

## 6. Definitions (Enums)

**OrderStatus**:
- `PENDING_MATCH`
- `DRIVER_ASSIGNED`
- `ACCEPTED`
- `ARRIVED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`
- `PAYMENT_SETTLED`

**VehicleType**:
- `ECONOMY`
- `STANDARD`
- `PREMIUM`

**ServiceType**:
- `NORMAL`
- `EXPRESS`
- `LUXURY`
