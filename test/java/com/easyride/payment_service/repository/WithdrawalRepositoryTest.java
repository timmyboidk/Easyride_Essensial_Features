package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Withdrawal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("提现仓库 (WithdrawalRepository) 测试")
class WithdrawalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Test
    @DisplayName("findByDriverId 应返回该司机的所有提现记录")
    void findByDriverId_shouldReturnAllWithdrawalsForDriver() {
        // Given
        Withdrawal w1 = new Withdrawal();
        w1.setDriverId(1L);
        w1.setAmount(100);

        Withdrawal w2 = new Withdrawal();
        w2.setDriverId(1L); // 同一个司机
        w2.setAmount(200);

        Withdrawal w3 = new Withdrawal();
        w3.setDriverId(2L); // 不同的司机
        w3.setAmount(300);

        entityManager.persist(w1);
        entityManager.persist(w2);
        entityManager.persist(w3);
        entityManager.flush();

        // When
        List<Withdrawal> withdrawals = withdrawalRepository.findByDriverId(1L);

        // Then
        assertThat(withdrawals).hasSize(2).containsExactlyInAnyOrder(w1, w2);
    }
}
