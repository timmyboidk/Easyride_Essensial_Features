# EasyRide Testing Checklist

## 1. Unit Tests

Target: Verify individual components (classes/methods) using JUnit 5 and Mockito.

### 1.1 User Service
- **AuthService**:
  - [ ] Register with valid phone/password -> Success.
  - [ ] Register with existing phone -> Fail.
  - [ ] Register with invalid format -> Fail.
  - [ ] Login with correct credentials -> Return JWT.
  - [ ] Login with wrong password -> Fail.
  - [ ] Request OTP with valid phone -> Success.
- **UserProfileService**:
  - [ ] Get profile of authenticated user -> Success.
  - [ ] Update profile (e.g., nickname) -> Success.
- **DriverService**:
  - [ ] Submit driver application -> Success.
  - [ ] Submit empty application -> Fail.

### 1.2 Order Service
- **OrderService**:
  - [ ] Create valid order -> Success.
  - [ ] Create order with invalid location -> Fail.
  - [ ] Idempotency Key prevents duplicate orders.
  - [ ] Get Order By ID -> Success.
  - [ ] Update status flow (PENDING -> MATCHED) -> Success.
  - [ ] Update status invalid flow (PENDING -> COMPLETED) -> Fail.
  - [ ] Cancel order -> Success.

### 1.3 Payment Service
- **PaymentService**:
  - [ ] Process payment via Gateway -> Success.
  - [ ] Process payment via Gateway -> Fail (Mocked).
  - [ ] Send `EASYRIDE_PAYMENT_SUCCESS_TOPIC` after success.
- **WalletService**:
  - [ ] Get Driver Wallet -> Success.
  - [ ] Request Withdrawal -> Success.
  - [ ] Withdrawal > Balance -> Fail.

### 1.4 Matching Service
- **MatchingEngine**:
  - [ ] Find nearest drivers (Redis Geo) -> Success.
  - [ ] No drivers found -> Return empty.
  - [ ] Consume `EASYRIDE_ORDER_CREATED_TOPIC` -> Trigger match.

## 2. Integration Tests

Target: Verify end-to-end business flows across microservices.

### Scenario 1: Full Ride Lifecycle
**Services**: User, Order, Matching, Location, Notification, Payment, Review.

1. **Passenger** logs in.
2. **Passenger** creates order (A to B).
3. **Matching** service receives event, starts search.
4. **Driver** comes online (Location update).
5. **Driver** accepts order.
6. **Order** status updates to `MATCHED`.
7. **Notification** sent to both parties.
8. **Driver** updates status: Arrved -> In Progress -> Completed.
9. **Payment** service triggers auto-deduction.
10. **Order** status updates to `PAID`.
11. **Passenger** leaves a 5-star review.

### Scenario 2: Driver Onboarding & Withdrawal
**Services**: User, Admin, Payment.

1. **Driver** registers and submits docs.
2. **Admin** sees "Pending Review" application.
3. **Admin** approves application.
4. **Driver** status becomes `ACTIVE`.
5. **Driver** completes rides, earns balance.
6. **Driver** requests withdrawal.
7. **Admin** approves withdrawal.
8. **Wallet** balance is deducted.

### Scenario 3: Order Cancellation
**Services**: Order, Payment.

1. **Passenger** creates order.
2. **Passenger** cancels immediately -> Status `CANCELLED`, No fee.
3. **Passenger** creates order -> Driver accepts.
4. **Passenger** cancels after 5 mins.
5. **System** calculates cancellation fee.
6. **Payment** processes fee deduction.

## 3. Boundary & Exception Testing

- **Matching Timeout**: No driver found within 3 mins -> Order cancelled/failed.
- **Payment Failure**: Gateway fails -> Order marked `PAYMENT_FAILED` -> User retries manually.
- **Geo-fencing**: Driver enters restricted zone -> Admin alerted.
