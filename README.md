# EasyRide Microservices

**EasyRide** is a cloud-native ride-hailing platform built with **Spring Boot** microservices, designed for high scalability and reliability.

## 🚀 Quick Start

### 1. Prerequisites
- Java 17
- Maven 3.8+
- Docker & Docker Compose

### 2. Infrastructure Setup
Spin up MySQL, Redis, RocketMQ, and Kafka:
```bash
cd infrastructure
docker-compose up -d
```

### 3. Build & Run
Build all services:
```bash
mvn clean install
```
Run a specific service:
```bash
cd user_service
mvn spring-boot:run
```

## 📖 Documentation

Full documentation is available in the `docs/` directory:

- **[Architecture Design](docs/ARCH_DESIGN.md)**: System overview, MQ, and DB design.
- **[API Reference](docs/API_REFERENCE.md)**: REST API specification.
- **[Setup Guide](docs/SETUP.md)**: Detailed installation and environment setup.
- **[Developer Guide](docs/DEV_GUIDE.md)**: Code style, testing, and contribution.
- **[DevOps & Deployment](docs/DEVOPS.md)**: Docker, CI/CD pipelines, and production deployment.
- **[Testing Checklist](docs/TESTING.md)**: Unit and Integration test plans.

## 🏗 Project Structure

```
EasyRide/
├── pom.xml                 # Root Parent POM
├── infrastructure/         # Docker Compose & Configs
├── docs/                   # Documentation
├── user_service/           # Identity & Profile
├── order_service/          # Order Management
├── payment_service/        # Wallet & Payments
├── matching_service/       # Driver Matching Engine
├── location_service/       # Geo-tracking
├── notification_service/   # SMS/Email/Push
├── review_service/         # Ratings
├── analytics_service/      # Data & Reporting
└── admin_service/          # Back-office Dashboard
```

## 🛠 Tech Stack
- **Framework**: Spring Boot 3.4.1
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Messaging**: Apache RocketMQ 5.x
- **Build**: Maven Multi-Module
- **Container**: Docker

---
&copy; 2026 EasyRide Team
