package com.easyride.user_service.repository;

import com.easyride.user_service.model.Passenger;
import com.easyride.user_service.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * UserRepository 的单元测试
 * @DataJpaTest 注解提供了JPA测试所需的所有环境：
 * 1. 配置一个嵌入式内存数据库 (H2)。
 * 2. 自动扫描 @Entity 类和 Spring Data JPA 的 Repository。
 * 3. 每个测试方法都在一个事务中运行，并在结束后回滚。
 */
@DataJpaTest
public class UserRepositoryTest {

    // TestEntityManager 是一个专门为测试设计的JPA EntityManager。
    // 我们可以用它来在测试执行前，准备数据库中的数据。
    @Autowired
    private TestEntityManager entityManager;

    // 将我们要测试的 UserRepository 注入进来。
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("当使用存在的用户名查询时, 应返回对应的用户")
    public void givenUserExists_whenFindByUsername_thenReturnsUser() {
        // --- Given (准备阶段) ---
        // 创建一个Passenger对象并保存到数据库
        User testUser = new Passenger("testuser", "password123", "test@example.com", "1234567890");
        entityManager.persistAndFlush(testUser); // persist保存，flush确保数据写入数据库

        // --- When (执行阶段) ---
        // 调用我们想要测试的方法
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // --- Then (验证阶段) ---
        // 使用 AssertJ 进行流式断言，验证结果是否符合预期
        assertThat(foundUser).isPresent(); // 断言Optional不为空
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("当使用不存在的用户名查询时, 应返回空的Optional")
    public void givenUserDoesNotExist_whenFindByUsername_thenReturnsEmpty() {
        // --- When ---
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // --- Then ---
        assertThat(foundUser).isNotPresent(); // 断言Optional为空
    }

    @Test
    @DisplayName("当手机号已存在时, existsByPhoneNumber应返回true")
    public void givenPhoneNumberExists_whenExistsByPhoneNumber_thenReturnsTrue() {
        // --- Given ---
        User testUser = new Passenger("anotheruser", "password123", "another@example.com", "9876543210");
        entityManager.persistAndFlush(testUser);

        // --- When ---
        boolean exists = userRepository.existsByPhoneNumber("9876543210");

        // --- Then ---
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("当邮箱已存在时, existsByEmail应返回true")
    public void givenEmailExists_whenExistsByEmail_thenReturnsTrue() {
        // --- Given ---
        User testUser = new Passenger("emailuser", "password123", "unique-email@example.com", "111222333");
        entityManager.persistAndFlush(testUser);

        // --- When ---
        boolean exists = userRepository.existsByEmail("unique-email@example.com");

        // --- Then ---
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("当尝试保存两个相同手机号的用户时, 应抛出数据完整性异常")
    public void whenSavingUserWithDuplicatePhoneNumber_thenThrowsException() {
        // --- Given ---
        User user1 = new Passenger("user1", "pass1", "user1@a.com", "1122334455");
        entityManager.persistAndFlush(user1);

        // --- When & Then ---
        User user2 = new Passenger("user2", "pass2", "user2@a.com", "1122334455");

        // 我们预期下面的代码会抛出 DataIntegrityViolationException 异常
        // 这是因为在User实体类中，phoneNumber字段被标记为 @Column(unique = true)
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}