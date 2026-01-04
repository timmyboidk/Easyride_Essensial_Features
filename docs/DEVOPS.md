# EasyRide 开发运维与部署指南

## 1. 容器化（Docker）

每个微服务根目录下都包含一个 `Dockerfile`。我们采用标准的两阶段构建流程，或直接使用简易的JDK运行时镜像。

### 1.1 构建服务镜像
```bash
cd user_service
docker build -t easyride/user-service:latest .
```

### 1.2 Dockerfile 标准模板
我们的 Dockerfile 通常遵循以下模式：
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT [“java”, “-jar”, “/app.jar”]
```

## 2. 持续集成/持续部署 (CI/CD)

建议使用 **GitHub Actions** 或 **Jenkins** 实现 CI/CD。

### 2.1 推荐 CI 管道
1. **触发条件**：推送至 `main` 分支或提交拉取请求。
2. **构建**：`mvn clean package -DskipTests`（验证）。
3. **测试**：`mvn test`（单元测试）。
4. **构建产物**：上传JAR文件或构建Docker镜像。

### 2.2 GitHub Action示例（`.github/workflows/maven.yml`）
（在仓库中创建此文件以启用 CI）

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ “main” ]
  pull_request:
    branches: [ “main” ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: 配置JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: ‘17’
        distribution: ‘temurin’
        cache: maven
    - name: Maven构建
      run: mvn -B package --file pom.xml
```

## 3. 生产环境部署

### 3.1 编排方案
生产环境推荐使用 **Kubernetes (K8s)** 或 **Docker Swarm**。

### 3.2 环境变量
确保在生产环境容器中注入以下变量：
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:mysql://<生产数据库>:3306/...`
- `SPRING_REDIS_HOST=<生产Redis实例>`
- `ROCKETMQ_NAME_SERVER=<生产消息服务器>:9876`

### 3.3 日志记录与监控
- **日志记录**：所有服务将日志输出至`STDOUT`。连接日志聚合器（ELK、Splunk、CloudWatch）。
- **指标监控**：已启用Spring Actuator。通过Prometheus + Grafana从`/actuator/prometheus`抓取指标数据。
