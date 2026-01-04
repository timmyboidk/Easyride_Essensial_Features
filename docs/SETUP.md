# EasyRide Setup & Installation

## 1. Prerequisites

Ensure your development environment meets the following requirements:
- **Java**: JDK 17 or higher
- **Maven**: 3.8+
- **Docker & Docker Compose**: Installed and running
- **Git**: For version control

## 2. Infrastructure Setup
We use Docker Compose to spin up all necessary middleware services (MySQL, Redis, RocketMQ, Kafka).

1. Navigate to the infrastructure directory:
   ```bash
   cd infrastructure
   ```

2. Start the services:
   ```bash
   docker-compose up -d
   ```

3. Verify services are running:
   ```bash
   docker ps
   ```
   You should see containers for `easyride-mysql`, `easyride-redis`, `rmqnamesrv`, `rmqbroker`, `easyride-zookeeper`, and `easyride-kafka`.

## 3. Database Initialization
The `docker-compose.yml` mounts `./mysql/init` to automatically initialize the MySQL database schemas on the first run.

If you need to manually reset the database:
1. Stop containers: `docker-compose down -v` (Removes volumes!)
2. Restart: `docker-compose up -d`

## 4. Application Configuration
Each service has an `application.yml` (or `properties`) file. By default, they are configured to connect to `localhost` ports mapped by Docker.

**Key Environment Variables** (Optional overrides):
- `SPRING_DATASOURCE_HOST`: Database host (default: localhost)
- `SPRING_REDIS_HOST`: Redis host (default: localhost)
- `ROCKETMQ_NAME_SERVER`: RocketMQ address (default: localhost:9876)

## 5. Running the Application

### Option A: Run from Command Line
You can run any service individually using Maven:

```bash
# Run User Service
cd user_service
mvn spring-boot:run
```

### Option B: Run in IDE (IntelliJ IDEA / Eclipse)
1. Open the root `pom.xml` as a project.
2. The IDE should detect all 9 modules.
3. Navigate to the main application class for the service you want to start (e.g., `UserServiceApplication.java`).
4. Right-click and select **Run**.

## 6. Verification
Once services are running, you can test the health endpoints:
- Admin Service: `http://localhost:8080/actuator/health`
- User Service: `http://localhost:8081/actuator/health`
...and so on.
