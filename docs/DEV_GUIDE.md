# EasyRide Developer Guide

## 1. Project Structure
This repository is a **Maven Multi-Module** project.

```
EasyRide_Essential_Features/
├── pom.xml                 # Root Parent POM (Dependencies & Plugin Management)
├── infrastructure/         # Centralized Middleware Config (Docker)
├── docs/                   # Documentation
├── user_service/           # Service: Users & Drivers
├── order_service/          # Service: Order Management
├── payment_service/        # Service: Payments & Wallets
├── ... (other services)
```

## 2. Building the Project

### 2.1 Full Build
To clean and build all modules:
```bash
mvn clean install
```
*Note: This runs unit tests by default.*

### 2.2 fast Build (Skip Tests)
To build quickly without running tests:
```bash
mvn clean install -DskipTests
```

## 3. Testing

### 3.1 Unit Tests
We use JUnit 5 and Mockito.
Run tests for a specific module:
```bash
mvn test -pl user_service
```

### 3.2 Key Testing libraries
- **JUnit 5**: Test runner.
- **Mockito**: Mocking dependencies.
- **H2 Database** (Optional): In-memory DB for repository testing (though we primarily use Dockerized MySQL for integration).

## 4. Code Style & Standards
- **Lombok**: Heavily used to reduce boilerplate (`@Data`, `@Builder`, `@Slf4j`).
- **DTO Pattern**: Always use DTOs for API requests/responses; never expose Entity classes directly.
- **Exceptions**: Use global exception handling (`@ControllerAdvice`) and custom `BizException`.
- **Formatting**: Standard Java Google Style or Checkstyle recommendations.

## 5. Adding a New Service
1. Create a new folder (e.g., `new_service`).
2. Add a `pom.xml` inheriting from `com.easyride:easyride-root`.
3. Add the module to the root `pom.xml` under `<modules>`.
4. Create the main Application class.
5. Create a `Dockerfile` (copy from existing service).
