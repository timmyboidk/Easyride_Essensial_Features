package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;
import org.springframework.dao.IncorrectResultSizeDataAccessException; // 需要导入这个异常类
import static org.assertj.core.api.Assertions.assertThatThrownBy; // 导入异常测试工具
import static org.assertj.core.api.Assertions.assertThat;

// 1. 使用 @DataJpaTest 注解来启用 JPA 相关的测试上下文
@DataJpaTest
@DisplayName("支付仓库 (PaymentRepository) 测试")
class PaymentRepositoryTest {

    // 2. 注入 TestEntityManager，这是我们与内存数据库交互的工具
    @Autowired
    private TestEntityManager entityManager;

    // 3. 注入我们要测试的 Repository
    @Autowired
    private PaymentRepository paymentRepository;


    @Test
    @DisplayName("当订单ID存在时，findByOrderId 应能返回对应的支付记录")
    void findByOrderId_shouldReturnPayment_whenOrderExists() {
        // --- Given (安排/准备) ---

        // 1. 创建一个新的 Payment 实体对象，但不设置 ID，因为数据库会自动生成
        Payment newPayment = new Payment();
        newPayment.setOrderId(1001L);
        newPayment.setPassengerId(1L);
        newPayment.setDriverId(88L);
        newPayment.setAmount(5000);
        newPayment.setStatus(PaymentStatus.COMPLETED);

        // 2. 使用 TestEntityManager 将这个实体持久化到内存数据库中。
        // persistAndFlush 会立即将数据写入数据库并返回持久化后的对象（现在它有ID了）
        entityManager.persistAndFlush(newPayment);

        // --- When (执行) ---

        // 3. 调用我们要测试的 Repository 方法
        Optional<Payment> foundPaymentOptional = paymentRepository.findByOrderId(1001L);

        // --- Then (断言/验证) ---

        // 4. 对查询结果进行断言
        assertThat(foundPaymentOptional).isPresent(); // 验证 Optional 不为空
        // 提取 Optional 中的值并进行进一步验证
        foundPaymentOptional.ifPresent(foundPayment -> {
            assertThat(foundPayment.getOrderId()).isEqualTo(1001L);
            assertThat(foundPayment.getPassengerId()).isEqualTo(1L);
            assertThat(foundPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(foundPayment.getId()).isNotNull(); // 验证数据库确实生成了ID
        });
    }

    @Test
    @DisplayName("当订单ID不存在时，findByOrderId 应返回空的 Optional")
    void findByOrderId_shouldReturnEmpty_whenOrderDoesNotExist() {
        // --- Given ---
        // 在这个测试中，我们不向数据库插入任何数据

        // --- When ---
        Optional<Payment> foundPaymentOptional = paymentRepository.findByOrderId(9999L);

        // --- Then ---
        assertThat(foundPaymentOptional).isNotPresent(); // 或者使用 .isEmpty()
    }


    @Test
    @DisplayName("当一个订单ID对应多条支付记录时，findByOrderId 应抛出异常")
    void findByOrderId_shouldThrowException_whenMultipleOrdersExist() {
        // --- Given (安排/准备) ---

        // 1. 创建两个具有相同 orderId 的 Payment 实体
        Payment payment1 = new Payment();
        payment1.setOrderId(1002L);
        payment1.setPassengerId(1L);
        payment1.setStatus(PaymentStatus.COMPLETED);

        Payment payment2 = new Payment();
        payment2.setOrderId(1002L); // 相同的 orderId
        payment2.setPassengerId(2L); // 其他数据可以不同
        payment2.setStatus(PaymentStatus.FAILED);

        // 2. 将这两个实体都持久化到内存数据库中
        entityManager.persistAndFlush(payment1);
        entityManager.persistAndFlush(payment2);

        // --- When & Then (执行与验证) ---

        // 3. 断言当调用 findByOrderId 时，会抛出 IncorrectResultSizeDataAccessException 异常
        assertThatThrownBy(() -> {
            paymentRepository.findByOrderId(1002L);
        })
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }
}