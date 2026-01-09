# EasyRide iOS (Swift) Passenger App Guide

This guide is designed to help Swift developers build a fully functional **Passenger App** for **EasyRide**. It covers architecture, networking, and a detailed breakdown of the passenger booking flow.

## 1. Aesthetics & Design Philosophy
**Goal**: Create a premium, Apple-native experience.
*   **Theme**: Pure white backgrounds (`Color.white`) with strong black typography (`Color.black`).
*   **Typography**: Use system fonts (San Francisco). Large, legible titles.
*   **Components**: Use standard SwiftUI components (Lists, Sheets, NavigationStacks) to ensure a familiar iOS feel.
*   **Icons**: Use **SF Symbols** specifically.
*   **Feel**: Smooth, fluid transitions. Use Apple Maps integration seamlessly.

## 2. Project Setup & Architecture
*   **Language**: Swift 5+
*   **UI Framework**: SwiftUI
*   **Architecture**: MVVM
*   **Networking**: `async/await` with `URLSession`
*   **Maps**: MapKit

## 3. Shared Data Models
(See `API_REFERENCE.md` for full DTOs).

### User & Profile
```swift
enum UserRole: String, Codable {
    case passenger = "PASSENGER"
    case driver = "DRIVER" // (Even if passenger app, backend returns this enum)
}

struct User: Codable {
    let userId: Int64
    let phoneNumber: String
    let nickname: String?
    let role: UserRole
    let accessToken: String?
}
```

### Order
```swift
enum OrderStatus: String, Codable {
    case pendingMatch = "PENDING_MATCH"
    case driverAssigned = "DRIVER_ASSIGNED"
    case accepted = "ACCEPTED"
    case arrived = "ARRIVED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case paid = "PAID"
    case cancelled = "CANCELLED"
}
```

## 4. Passenger App Flow

### 4.1 Authentication & Profile
*   **Login (Password)**: `POST /api/user/auth/login/password`
*   **Login (OTP)**: `POST /api/user/auth/login/otp` (Requires `POST /api/user/auth/otp/request` first)
*   **Register**: `POST /api/user/auth/register` (Role: `PASSENGER`)
*   **Forgot Password**: `POST /api/user/auth/password/reset`
*   **Get Profile**: `GET /api/user/profile/`
*   **Update Profile**: `PUT /api/user/profile/`

### 4.2 Home (Map & Destination)
*   **UI**: Full-screen Map with Floating "Where to?" Search Bar.
*   **Action**: User searches -> Selects result -> Transitions to Ride Config.

### 4.3 Ride Configuration
*   **UI**: Route Preview, Service List (Economy/Premium), Price Estimates.
*   **API**: `POST /api/order/estimate-price`
*   **Action**: "Request Ride" -> Calls `POST /api/order/` -> Transitions to Matching.

### 4.4 Matching & Waiting
*   **UI**: "Finding your driver..."
*   **Logic**: Poll `GET /api/order/{id}`.
    *   **Cancel**: User can cancel via `POST /api/order/{id}/cancel`.
    *   **Transition**: When status becomes `DRIVER_ASSIGNED` or `ACCEPTED`, go to **Trip View**.

### 4.5 On Trip
*   **UI**: Driver Info, Live Map, ETA.
*   **API**:
    *   **Driver Loc**: `GET /api/location/order/{id}` (Poll every 3s).
    *   **Order Status**: Poll `GET /api/order/{id}`.
*   **States**: Driver Arriving -> Driver Arrived -> In Progress -> Arrived at Destination.

### 4.6 Payment & Rating
*   **Trigger**: Order Status `COMPLETED`.
*   **Payment Methods**:
    *   List: `GET /api/payment/methods/`
    *   Add: `POST /api/payment/methods/`
    *   Delete: `DELETE /api/payment/methods/{methodId}`
*   **Pay**: `POST /api/payment/payments/`
*   **Review**: `POST /api/review/`
    *   **Complaint**: `POST /api/review/complaints` (Optional)

### 4.7 History
*   **View History**: `GET /api/order/history?page=0&size=10`

## 5. Development Tips
*   **MapKit**: Use `MapPolyline` for routes. Use `Annotation` for Driver.
*   **Error Handling**: Show clean Alerts for network errors.

## 6. AI Co-Pilot Prompt (Gemini Pro)

Use this prompt to help refactor your code for the **Passenger App**:

> Act as a Senior iOS Engineer specializing in Consumer/Passenger apps.
>
> **Goal**: Refactor my existing Swift code to build the "EasyRide Passenger App".
>
> **Design Rules**:
> 1.  **Apple Native**: Use strict Apple Human Interface Guidelines. White backgrounds, Black text, SF Symbols.
> 2.  **Clean UI**: Minimalist design. Focus on content and map. Standard SwiftUI navigation.
>
> **Functionality**:
> 1.  Implement the full **Passenger Flow** defined in `FRONTEND_GUIDE.md`.
> 2.  Integrate **ALL** endpoints: Auth (OTP/Pwd), Order Cycle (Create/Cancel/Track), Payment Methods, and History.
> 3.  Implement Real-time Polling for Driver Location (`/api/location/order/{id}`).
>
> **Task**: Refactor the provided file to implement the **[Insert Feature]** complying with the EasyRide Passenger API.
