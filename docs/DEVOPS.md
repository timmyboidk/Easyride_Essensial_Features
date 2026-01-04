# EasyRide DevOps & Deployment Guide

## 1. Containerization (Docker)

Each microservice includes a `Dockerfile` in its root directory. We use a standard 2-stage build or a simple JDK runtime image.

### 1.1 Building a Service Image
```bash
cd user_service
docker build -t easyride/user-service:latest .
```

### 1.2 Dockerfile Standard
Our Dockerfiles generally follow this pattern:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 2. Continuous Integration / Continuous Deployment (CI/CD)

We recommend using **GitHub Actions** or **Jenkins** for CI/CD.

### 2.1 Proposed CI Pipeline
1. **Trigger**: Push to `main` or Pull Request.
2. **Build**: `mvn clean package -DskipTests` (Validation).
3. **Test**: `mvn test` (Unit Tests).
4. **Artifact**: Upload JARs or build Docker images.

### 2.2 Example GitHub Action (`.github/workflows/maven.yml`)
(Create this file in your repo to enable CI)

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    - name: Build with Maven
      run: mvn -B package --file pom.xml
```

## 3. Production Deployment

### 3.1 Orchestration
For production, we recommend **Kubernetes (K8s)** or **Docker Swarm**.

### 3.2 Environment Variables
Ensure the following variables are injected into containers in production:
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:mysql://<prod-db>:3306/...`
- `SPRING_REDIS_HOST=<prod-redis>`
- `ROCKETMQ_NAME_SERVER=<prod-namesrv>:9876`

### 3.3 Logging & Monitoring
- **Logging**: All services output logs to `STDOUT`. Connect a log aggregator (ELK, Splunk, CloudWatch).
- **Metrics**: Spring Actuator is enabled. Scrape metrics from `/actuator/prometheus` using Prometheus + Grafana.
