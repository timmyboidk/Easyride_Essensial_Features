# EasyRide 开发者指南

## 1. 项目结构
本仓库为**Maven多模块**项目。

```
EasyRide_Essential_Features/
├── pom.xml                 # 根父级POM（依赖与插件管理）
├── infrastructure/         # 集中式中间件配置（Docker）
├── docs/                   # 文档
├── user_service/           # 服务：用户与司机
├── order_service/          # 服务：订单管理
├── payment_service/        # 服务：支付与钱包
├── ... (其他服务)
```

## 2. 构建项目

### 2.1 全量构建
清理并构建所有模块：
```bash
mvn clean install
```
*注意：默认会运行单元测试*

### 2.2 快速构建（跳过测试）
快速构建且不运行测试：
```bash
mvn clean install -DskipTests
```

## 3. 测试

### 3.1 单元测试
采用 JUnit 5 与 Mockito 框架。
运行特定模块测试：
```bash
mvn test -pl user_service
```

### 3.2 核心测试库
- **JUnit 5**：测试运行器。
- **Mockito**：模拟依赖项。
- **H2数据库**（可选）：用于仓库测试的内存数据库（但集成测试主要采用Docker化的MySQL）。

## 4. 代码风格与规范
- **Lombok**：广泛用于减少冗余代码（`@Data`、`@Builder`、`@Slf4j`）。
- **DTO模式**：API请求/响应必须使用DTO，严禁直接暴露实体类。
- **异常处理**：采用全局异常处理（`@ControllerAdvice`）并使用自定义`BizException`。
- **代码格式**：遵循标准Java Google风格或Checkstyle规范。

## 5. 新增服务流程
1. 创建新文件夹（例如`new_service`）。
2. 添加继承自`com.easyride:easyride-root`的`pom.xml`。
3. 在根`pom.xml`的`<modules>`下添加该模块。
4. 创建主应用程序类。
5. 创建`Dockerfile`（从现有服务复制）。
