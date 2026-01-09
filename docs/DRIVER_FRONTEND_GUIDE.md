# EasyRide iOS (Swift) Driver App Guide

This guide is designed to help Swift developers build a fully functional **Driver App** for **EasyRide**. It covers architecture, networking, and a detailed breakdown of driver-specific workflows.

## 1. Aesthetics & Design Philosophy
**Goal**: Create a premium, Apple-native experience.
*   **Theme**: Pure white backgrounds (`Color.white`) with strong black typography (`Color.black`).
*   **Typography**: Use system fonts (San Francisco). Large, bold titles for key statuses.
*   **Components**: Use standard SwiftUI components (Lists, Sheets, NavigationStacks).
*   **Icons**: Use **SF Symbols** specifically.
*   **Simplicity**: Primary actions (Accept, Arrived) should be prominent buttons.

## 2. Project Setup & Architecture
*   **Language**: Swift 5+
*   **UI Framework**: SwiftUI
*   **Architecture**: MVVM
*   **Networking**: `async/await` with `URLSession`
*   **Maps**: MapKit

## 3. Shared Data Models
(See `FRONTEND_GUIDE.md` for shared models, but include `DriverInfo`, `Wallet`, `Transaction`).

## 4. Driver App Flow

### 4.1 Authentication & Onboarding
*   **Login**: `POST /api/user/auth/login/password` (or OTP)
*   **Register (Apply)**: `POST /api/user/driver/register`
    *   **Body**: Real Name, ID Card, License, Car Info, Attachments (Multipart).
*   **Status Check**: Drivers must wait for Admin approval. Handle "Pending" state on login.
*   **Profile**: `GET/PUT /api/user/profile/`

### 4.2 Dashboard (Home)
*   **State**: Offline vs. Online.
*   **UI**: Map + "Go Online" Slider.
*   **API**:
    *   **Set Status**: `POST /api/matching/driver/status` (`ONLINE`/`OFFLINE`)
    *   **Ping Location**: `POST /api/location/driver/update` (Every 5s when ONLINE).
    *   **Get Orders**: `GET /api/matching/driver/orders` (Poll available jobs).

### 4.3 Accepting Orders
*   **Trigger**: Order appears in polling list or Push Notification.
*   **UI**: Bottom Sheet with Trip Details (Fare, Distance).
*   **Action**: "Accept Ride" -> `POST /api/matching/driver/grab` (`orderId`).

### 4.4 In-Trip Navigation
*   **State Machine**:
    1.  **To Pickup**: `PUT /api/order/{id}/status` -> `ARRIVED`.
    2.  **Wait**: `PUT /api/order/{id}/status` -> `IN_PROGRESS` (Start Trip).
    3.  **To Dropoff**: `PUT /api/order/{id}/status` -> `COMPLETED` (End Trip).
*   **Navigation**: Deep link to Google Maps/Apple Maps for turn-by-turn.

### 4.5 Wallet & Earnings
*   **Balance**: `GET /api/payment/wallet/`
*   **Transactions**: `GET /api/payment/wallet/transactions` (History)
*   **Withdraw**: `POST /api/payment/wallet/withdrawals`
    *   **Body**: Amount, Method ID.

### 4.6 History & Ratings
*   **Order History**: `GET /api/order/history`
*   **Reviews**: Check Profile or specialized endpoint if available.

## 5. AI Co-Pilot Prompt (Gemini Pro)

Use this prompt to help refactor your code for the **Driver App**:

> Act as a Senior iOS Engineer specializing in Driver/Gig-economy apps.
>
> **Goal**: Refactor my existing Swift code to build the "EasyRide Driver App".
>
> **Design Rules**:
> 1.  **Apple Native**: Use strict Apple Human Interface Guidelines. White backgrounds, Black text, SF Symbols.
> 2.  **Clean UI**: No custom gradients or shadows. Use standard SwiftUI modifiers.
>
> **Functionality**:
> 1.  Implement the **Driver Flow** defined in `DRIVER_FRONTEND_GUIDE.md`.
> 2.  Integrate **ALL** endpoints: Onboarding, Online/Offline, Location Ping, Order Accepting, Trip Status Updates, and Wallet/Withdrawal.
> 3.  Ensure **Background Location Updates** are handled correctly using `CoreLocation`.
>
> **Task**: Refactor the provided file to implement the **[Insert Feature]** complying with the EasyRide Driver API.
