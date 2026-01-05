package com.easyride.analytics_service.integration;

import com.easyride.analytics_service.config.TestSecurityConfig;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for analytics service integration tests.
 * 
 * Sets up Testcontainers for:
 * - MySQL: Real database for MyBatis-Plus persistence
 * - Redis: Real Redis for HyperLogLog DAU/MAU tracking
 * 
 * All integration test classes should extend this base class to inherit
 * the container configuration.
 * 
 * Uses static containers (singleton pattern) to improve test performance
 * by reusing containers across all test classes in the same JVM.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AnalyticsIntegrationTestBase {

    /**
     * MySQL container for testing database persistence.
     * Uses MySQL 8.0 with a test database.
     * Static to be shared across all test classes (singleton pattern).
     */
    @Container
    protected static MySQLContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("analytics_test_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * Redis container for testing HyperLogLog operations (DAU/MAU).
     * Uses Redis 7 Alpine for lightweight testing.
     * Static to be shared across all test classes (singleton pattern).
     */
    @Container
    protected static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withReuse(true);

    // Static initialization block for containers
    static {
        mysqlContainer.start();
        redisContainer.start();
    }

    /**
     * Dynamically configure Spring properties to use the Testcontainers.
     * This overrides the application-test.yml placeholders with actual container
     * URLs.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Enable schema initialization
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema.sql");

        // Redis configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        // Disable RocketMQ auto-configuration for tests
        registry.add("rocketmq.name-server", () -> "disabled:9876");
    }
}
